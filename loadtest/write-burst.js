// Write-burst scenario: proves the rate limiter sheds abusive POST traffic with 429s.
// Run:  k6 run loadtest/write-burst.js   (app must be running on localhost:8080)
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const created = new Counter('pastes_created');
const limited = new Counter('pastes_rate_limited');

export const options = {
    vus: 5,
    duration: '30s',
    thresholds: {
        // the limiter must kick in: expect some 429s during a burst
        pastes_rate_limited: ['count>0'],
    },
};

export default function () {
    const res = http.post(`${BASE}/api/pastes`,
        JSON.stringify({ content: `burst ${Date.now()}` }),
        { headers: { 'Content-Type': 'application/json' } });
    check(res, { 'created or limited': (r) => r.status === 201 || r.status === 429 });
    if (res.status === 201) created.add(1);
    if (res.status === 429) limited.add(1);
}
