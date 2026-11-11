import { describe, expect, it } from 'vitest';
import { fmt, label, toDate } from '../ui.jsx';

describe('label', () => {
  it('turns enum names into readable text', () => {
    expect(label('IN_PROGRESS')).toBe('In progress');
    expect(label('CRITICAL')).toBe('Critical');
  });
  it('handles empty values', () => {
    expect(label('')).toBe('');
    expect(label(undefined)).toBe('');
  });
});

describe('toDate', () => {
  it('treats timestamps without a zone as UTC (that is what the API sends)', () => {
    expect(toDate('2026-11-11T10:30:00').getUTCHours()).toBe(10);
  });
  it('keeps explicit zones as they are', () => {
    expect(toDate('2026-11-11T10:30:00Z').getTime()).toBe(toDate('2026-11-11T10:30:00').getTime());
    expect(toDate('2026-11-11T16:00:00+05:30').getTime()).toBe(toDate('2026-11-11T10:30:00').getTime());
  });
  it('returns null for missing values', () => {
    expect(toDate(null)).toBeNull();
  });
});

describe('fmt', () => {
  it('shows a dash for missing dates', () => {
    expect(fmt(null)).toBe('—');
  });
});
