import { Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Task, TaskFormValue, TaskStatus } from '../../../core/models/task.model';

@Component({
  selector: 'app-task-form',
  imports: [ReactiveFormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.scss',
})
export class TaskForm {
  private readonly fb = inject(FormBuilder);

  /** When set, the form edits this task; when null, it creates a new one. */
  readonly task = input<Task | null>(null);
  readonly save = output<TaskFormValue>();
  readonly cancelled = output<void>();

  readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    status: this.fb.nonNullable.control<TaskStatus>('TODO', Validators.required),
  });

  constructor() {
    effect(() => {
      const task = this.task();
      this.form.reset({
        title: task?.title ?? '',
        description: task?.description ?? '',
        status: task?.status ?? 'TODO',
      });
    });
  }

  get titleControl() {
    return this.form.controls.title;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.save.emit({
      title: value.title,
      description: value.description ? value.description : undefined,
      status: value.status,
    });
  }
}
