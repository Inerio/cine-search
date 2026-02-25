/** Computes visible page numbers with ellipsis markers (-1) for pagination UI. */
export function computeVisiblePages(total: number, current: number): number[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: number[] = [1];
  if (current > 3) pages.push(-1);
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i);
  }
  if (current < total - 2) pages.push(-1);
  if (pages[pages.length - 1] !== total) pages.push(total);
  return pages;
}
