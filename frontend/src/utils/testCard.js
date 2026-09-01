// Dev-only helper for local Razorpay test-mode runs.
//
// Razorpay's Checkout is their own hosted iframe and there is no supported
// way to pre-fill the card number from this page (deliberate, for PCI
// compliance) -- `prefill` only accepts email/contact/name/method. So the
// best we can do is keep a chosen test card saved locally and make it
// one-click copyable into their form. Everything here is stripped from
// production builds (callers gate on `import.meta.env.DEV`).

const STORAGE_KEY = 'skyfare-test-card'

// Razorpay's documented domestic (India) success card.
export const DEFAULT_TEST_CARD = {
  number: '5267 3181 8797 5449',
  network: 'Mastercard · domestic',
  expiry: '12 / 30',
  cvv: '123',
  otp: '1111',
}

export function loadTestCard() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_TEST_CARD }
    const saved = JSON.parse(raw)
    return { ...DEFAULT_TEST_CARD, ...saved }
  } catch {
    return { ...DEFAULT_TEST_CARD }
  }
}

export function saveTestCard(card) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(card))
  } catch {
    // Private window / storage disabled -- the card just won't persist.
  }
}

export async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}
