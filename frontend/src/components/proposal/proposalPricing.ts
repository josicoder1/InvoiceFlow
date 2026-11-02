export interface PricedItem {
  description: string;
  quantity?: number | null;
  unitPrice?: number | null;
  total?: number | null;
}

export function calcSubtotal(items: PricedItem[]): number {
  return items.reduce((sum, item) => {
    if (item.total != null) return sum + item.total;
    return sum + (item.quantity ?? 0) * (item.unitPrice ?? 0);
  }, 0);
}

export function calcDiscountAmount(
  subtotal: number,
  discountType?: 'PERCENT' | 'FIXED' | string | null,
  discountValue?: number | null,
): number {
  if (!discountType || !discountValue) return 0;
  if (discountType === 'PERCENT') return subtotal * discountValue / 100;
  return Math.min(discountValue, subtotal);
}

export function calcTaxAmount(subtotal: number, discount: number, taxRate?: number | null): number {
  if (!taxRate) return 0;
  return (subtotal - discount) * taxRate / 100;
}

export function calcTotal(
  subtotal: number,
  discountType?: 'PERCENT' | 'FIXED' | string | null,
  discountValue?: number | null,
  taxRate?: number | null,
): number {
  const discount = calcDiscountAmount(subtotal, discountType, discountValue);
  const tax = calcTaxAmount(subtotal, discount, taxRate);
  return subtotal - discount + tax;
}
