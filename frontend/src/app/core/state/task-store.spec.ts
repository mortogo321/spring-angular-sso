import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { environment } from '../../../environments/environment';
import { Task } from '../models/task.model';
import { TaskStore } from './task-store';

const apiUrl = `${environment.apiUrl}/tasks`;

const sampleTask: Task = {
  id: '11111111-1111-1111-1111-111111111111',
  title: 'Write tests',
  description: 'Cover the task store',
  status: 'TODO',
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-01T00:00:00.000Z',
};

describe('TaskStore', () => {
  let store: TaskStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    store = TestBed.inject(TaskStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts with empty, non-loading state', () => {
    expect(store.tasks()).toEqual([]);
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('loads tasks and populates the store', () => {
    store.load();
    expect(store.loading()).toBe(true);

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([sampleTask]);

    expect(store.loading()).toBe(false);
    expect(store.tasks()).toEqual([sampleTask]);
    expect(store.error()).toBeNull();
  });

  it('sets an error message when loading fails', () => {
    store.load();

    const req = httpMock.expectOne(apiUrl);
    req.flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('boom');
    expect(store.tasks()).toEqual([]);
  });

  it('appends a created task to the store', () => {
    store.create({ title: 'New task', status: 'TODO' });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    req.flush(sampleTask);

    expect(store.tasks()).toEqual([sampleTask]);
  });

  it('replaces an updated task in the store', () => {
    store.create({ title: 'New task', status: 'TODO' });
    httpMock.expectOne(apiUrl).flush(sampleTask);

    const updated: Task = { ...sampleTask, status: 'DONE' };
    store.update(sampleTask.id, { title: sampleTask.title, status: 'DONE' });

    const req = httpMock.expectOne(`${apiUrl}/${sampleTask.id}`);
    expect(req.request.method).toBe('PUT');
    req.flush(updated);

    expect(store.tasks()).toEqual([updated]);
  });

  it('removes a task from the store', () => {
    store.create({ title: 'New task', status: 'TODO' });
    httpMock.expectOne(apiUrl).flush(sampleTask);

    store.remove(sampleTask.id);

    const req = httpMock.expectOne(`${apiUrl}/${sampleTask.id}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(store.tasks()).toEqual([]);
  });
});
