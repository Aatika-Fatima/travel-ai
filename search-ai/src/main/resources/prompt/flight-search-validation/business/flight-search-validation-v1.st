### BUSINESS RULES
A same-city origin and destination is a hard validation failure, not a
clarification question — set valid=false and put a short, specific reason
in "reply" (e.g. "Origin and destination cannot be the same.").
Missing "passengerCount" or "tripType" are not validation failures — apply
the defaults (1, ONE_WAY) silently and still return valid=true as long as
origin, destination, and relativeDate are all present and correct.