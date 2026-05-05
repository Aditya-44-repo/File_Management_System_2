import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { SearchCriteria } from '../../models/search-criteria.model';

@Component({
  selector: 'app-file-search',
  templateUrl: './file-search.component.html',
  styleUrls: ['./file-search.component.scss']
})
export class FileSearchComponent implements OnInit, OnDestroy {
  @Output() search = new EventEmitter<SearchCriteria>();
  isFilterActive = false;
  private formChangesSub?: Subscription;

  form = this.fb.group({
    fileId: [''],
    filename: [''],
    status: [''],
    startDate: [''],
    endDate: [''],
    recordCountMin: [''],
    recordCountMax: ['']
  });

  today = new Date();

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.formChangesSub = this.form.valueChanges
      .pipe(debounceTime(300))
      .subscribe(() => {
        const wasFilterActive = this.isFilterActive;
        this.isFilterActive = this.hasAnyFilterValue();

        // If user manually clears all filters, restore the default unfiltered list automatically.
        if (wasFilterActive && !this.isFilterActive) {
          this.emitDefaultSearch();
        } else {
          this.submit(); // Auto-filter on any change
        }
      });
  }

  ngOnDestroy(): void {
    this.formChangesSub?.unsubscribe();
  }

  private toLocalDateTime(value: unknown, endOfDay = false): string | undefined {
    if (!value) return undefined;

    const d = value instanceof Date ? value : new Date(value as string);
    if (Number.isNaN(d.getTime())) return undefined;

    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hh = endOfDay ? '23' : '00';
    const mm = endOfDay ? '59' : '00';
    const ss = endOfDay ? '59' : '00';
    return `${y}-${m}-${day}T${hh}:${mm}:${ss}`;
  }

  submit() {
    this.isFilterActive = this.hasAnyFilterValue();
    const { fileId, filename, status, startDate, endDate, recordCountMin, recordCountMax } = this.form.value;
    const min = recordCountMin !== '' && recordCountMin !== null ? Number(recordCountMin) : undefined;
    const max = recordCountMax !== '' && recordCountMax !== null ? Number(recordCountMax) : undefined;

    const normalizedStatus = status === 'COMPLETED' ? 'SUCCESS' : status || '';

    this.search.emit({
      fileId: fileId || '',
      filename: filename || '',
      status: normalizedStatus,
      startDate: this.toLocalDateTime(startDate, false),
      endDate: this.toLocalDateTime(endDate, true),
      recordCountMin: min,
      recordCountMax: max,
      page: 0
    });
  }

  reset() {
    this.isFilterActive = false;
    this.form.reset({
      fileId: '',
      filename: '',
      status: '',
      startDate: '',
      endDate: '',
      recordCountMin: '',
      recordCountMax: ''
    });
    this.emitDefaultSearch();
  }

  private emitDefaultSearch(): void {
    this.search.emit({
      fileId: '',
      filename: '',
      status: '',
      startDate: undefined,
      endDate: undefined,
      recordCountMin: undefined,
      recordCountMax: undefined,
      page: 0
    });
  }

  private hasAnyFilterValue(): boolean {
    const value = this.form.value;
    return Object.values(value).some((fieldValue) => {
      if (fieldValue === null || fieldValue === undefined) {
        return false;
      }
      const normalized = String(fieldValue).trim();
      return normalized.length > 0;
    });
  }
}