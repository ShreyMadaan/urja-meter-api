# Observed upstream protocol

All endpoints are relative to `https://urja-ops.flockenergy.tech`. Observations were made through authorized browser DevTools investigation; details may change.

| Request | Response / observations |
|---|---|
| `POST /login` | `application/x-www-form-urlencoded` fields `email`, `password`; HTTP 200 with JSON redirect instruction and `Set-Cookie: __Secure-better-auth.session_token=...`; observed `Max-Age=3600`, `HttpOnly`, `Secure`, `SameSite=Lax` |
| `GET /portal/meters/search?q=&page=1` | JSON `data[]` with `meterId`, `serialNo`, `make`, `phaseType`, `installStatus`, `dtCode`; `total`, `page`, `pageSize`; observed page size 20, total 403 |
| `GET /meters/{meterId}` | SvelteKit server-rendered HTML containing nameplate and hierarchy, not consumed by this implementation |
| `GET /portal/meters/{meterId}/energy` | JSON `data[]` with timestamp formatted `dd/MM/yyyy HH:mm`, and string fields `kwh`, `kvah`, `voltR`; observed half-hour samples; timezone and measurement semantics unconfirmed |
| `GET /portal/meters/{meterId}/geo` | JSON `data` with `latitude` and `longitude` as strings |
| `GET /portal/dts?page=1` | JSON `data[]` of transformer `code`, `name`, `feederCode`, `capacityKva`; observed 20 per page |
| `GET /portal/export?page=1` | JSON `data[]` of meter metadata including `installType`, nested `hierarchy`, and `geo` (`lat`, `lng` numeric), plus `total`; observed total 403, but exhaustive page coverage not independently established |

All portal data endpoints require the login session cookie. The client stores only the session cookie in memory, sends it via `Cookie`, and retries once on 401 with a refreshed session.

**Assumptions and discrepancies:** export page 1 appeared to provide a bulk dataset, but its pagination behavior must be verified. A transformer name differed between export and transformer listing for the same code in the observed dataset. Treat code as the stable join key and avoid claiming name consistency. Never publish credentials, session cookies, or any signing secret exposed by the portal.
