export async function sendAssistantMessage(message) {
  const res = await fetch('/api/v1/assistant/message', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  })

  if (!res.ok) {
    throw new Error(`Assistant request failed (HTTP ${res.status})`)
  }

  return res.json()
}
