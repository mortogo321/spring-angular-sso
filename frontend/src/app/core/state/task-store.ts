import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { environment } from '../../../environments/environment';
import { Task, TaskFormValue } from '../models/task.model';

interface TaskState {
  tasks: Task[];
  loading: boolean;
  error: string | null;
}

const initialState: TaskState = {
  tasks: [],
  loading: false,
  error: null,
};

/**
 * Signal-based store for the tasks feature. Owns the loading/error/data
 * state and talks to the backend `/api/tasks` endpoints directly, so
 * components only ever read signals and call intention-revealing methods.
 */
@Injectable({ providedIn: 'root' })
export class TaskStore {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/tasks`;

  private readonly state = signal<TaskState>(initialState);

  readonly tasks = computed(() => this.state().tasks);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);

  load(): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));

    this.http.get<Task[]>(this.apiUrl).subscribe({
      next: (tasks) => this.state.update((s) => ({ ...s, tasks, loading: false })),
      error: (err: unknown) =>
        this.state.update((s) => ({ ...s, loading: false, error: this.toMessage(err) })),
    });
  }

  create(payload: TaskFormValue): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));

    this.http.post<Task>(this.apiUrl, payload).subscribe({
      next: (task) =>
        this.state.update((s) => ({ ...s, tasks: [...s.tasks, task], loading: false })),
      error: (err: unknown) =>
        this.state.update((s) => ({ ...s, loading: false, error: this.toMessage(err) })),
    });
  }

  update(id: string, payload: TaskFormValue): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));

    this.http.put<Task>(`${this.apiUrl}/${id}`, payload).subscribe({
      next: (updated) =>
        this.state.update((s) => ({
          ...s,
          tasks: s.tasks.map((task) => (task.id === updated.id ? updated : task)),
          loading: false,
        })),
      error: (err: unknown) =>
        this.state.update((s) => ({ ...s, loading: false, error: this.toMessage(err) })),
    });
  }

  remove(id: string): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));

    this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe({
      next: () =>
        this.state.update((s) => ({
          ...s,
          tasks: s.tasks.filter((task) => task.id !== id),
          loading: false,
        })),
      error: (err: unknown) =>
        this.state.update((s) => ({ ...s, loading: false, error: this.toMessage(err) })),
    });
  }

  private toMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      return err.error?.message ?? err.message ?? 'Request failed';
    }
    return 'Unexpected error';
  }
}
