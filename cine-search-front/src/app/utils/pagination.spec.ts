import { computeVisiblePages } from './pagination';

describe('computeVisiblePages', () => {
  it('should return all pages when total <= 7', () => {
    expect(computeVisiblePages(5, 3)).toEqual([1, 2, 3, 4, 5]);
  });

  it('should return [1..7] for exactly 7 pages', () => {
    expect(computeVisiblePages(7, 4)).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });

  it('should return empty array for 0 total pages', () => {
    expect(computeVisiblePages(0, 1)).toEqual([]);
  });

  it('should show ellipsis at start when current > 3', () => {
    const pages = computeVisiblePages(10, 6);
    expect(pages[0]).toBe(1);
    expect(pages[1]).toBe(-1); // ellipsis
    expect(pages).toContain(6);
  });

  it('should show ellipsis at end when current < total - 2', () => {
    const pages = computeVisiblePages(10, 3);
    expect(pages[pages.length - 1]).toBe(10);
    expect(pages).toContain(-1);
  });

  it('should show both ellipses when in the middle', () => {
    const pages = computeVisiblePages(20, 10);
    expect(pages[0]).toBe(1);
    expect(pages[1]).toBe(-1);
    expect(pages).toContain(9);
    expect(pages).toContain(10);
    expect(pages).toContain(11);
    expect(pages[pages.length - 1]).toBe(20);
    // Count ellipsis markers
    expect(pages.filter(p => p === -1).length).toBe(2);
  });

  it('should not show leading ellipsis when on page 1', () => {
    const pages = computeVisiblePages(10, 1);
    expect(pages[0]).toBe(1);
    expect(pages[1]).toBe(2); // no ellipsis after first page
  });

  it('should not show trailing ellipsis when on last page', () => {
    const pages = computeVisiblePages(10, 10);
    expect(pages[pages.length - 1]).toBe(10);
    expect(pages[pages.length - 2]).not.toBe(-1);
  });

  it('should always include first and last page', () => {
    for (const current of [1, 5, 10, 15, 20]) {
      const pages = computeVisiblePages(20, current);
      expect(pages[0]).toBe(1);
      expect(pages[pages.length - 1]).toBe(20);
    }
  });
});
