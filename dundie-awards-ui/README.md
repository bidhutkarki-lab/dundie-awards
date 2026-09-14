# Dundie Awards UI

React frontend for the Dundie Awards application, built with Vite and TypeScript.

## Running

```bash
npm install
npm run dev
```

The dev server runs at <http://localhost:5173>.

| Script | Purpose |
| --- | --- |
| `npm run dev` | Start the dev server with hot reload |
| `npm run build` | Type-check and build to `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run oxlint |

## Backend

The API lives in [`../dundie-awards-service`](../dundie-awards-service) and serves at
<http://localhost:3000>. Start it with `./gradlew bootRun` from that folder (Postgres must be
running — `docker compose up -d db`).
