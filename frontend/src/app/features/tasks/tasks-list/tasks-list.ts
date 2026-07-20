import { Component, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

import { Task, TaskFormValue } from '../../../core/models/task.model';
import { TaskStore } from '../../../core/state/task-store';
import { TaskForm } from '../task-form/task-form';

@Component({
  selector: 'app-tasks-list',
  imports: [TaskForm],
  templateUrl: './tasks-list.html',
  styleUrl: './tasks-list.scss',
})
export class TasksList {
  private readonly store = inject(TaskStore);
  private readonly keycloak = inject(Keycloak);

  readonly tasks = this.store.tasks;
  readonly loading = this.store.loading;
  readonly error = this.store.error;

  readonly isAdmin = computed(() => this.keycloak.hasRealmRole('admin'));

  readonly showForm = signal(false);
  readonly editingTask = signal<Task | null>(null);

  constructor() {
    this.store.load();
  }

  statusClass(status: Task['status']): string {
    return `badge badge--${status.toLowerCase()}`;
  }

  startCreate(): void {
    this.editingTask.set(null);
    this.showForm.set(true);
  }

  startEdit(task: Task): void {
    this.editingTask.set(task);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingTask.set(null);
  }

  save(value: TaskFormValue): void {
    const editing = this.editingTask();
    if (editing) {
      this.store.update(editing.id, value);
    } else {
      this.store.create(value);
    }
    this.showForm.set(false);
    this.editingTask.set(null);
  }

  remove(task: Task): void {
    this.store.remove(task.id);
  }
}
