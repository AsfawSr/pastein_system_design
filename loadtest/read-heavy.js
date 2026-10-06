// Read-heavy scenario: pastebin traffic is ~95% reads.
// Run:  k6 run loadtest/read-heavy.js   (app must be running on localhost:8080)
// Watch cache + HTTP metrics at http://localhost:8080/actuator/metrics while it runs.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '30s', target: 50 },  // ramp up
        { duration: '1m', target: 50 },   // hold
        { duration: '15s', target: 0 },   // ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<300'], // 95% of reads under 300ms
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const res = http.post(`${BASE}/api/pastes`,
        JSON.stringify({ title: 'load target', content: 'content served from cache, hopefully' }),
        { headers: { 'Content-Type': 'application/json' } });
    return { id: res.json('id') };
}

export default function (data) {
    const res = http.get(`${BASE}/api/pastes/${data.id}`);
    check(res, { 'read ok': (r) => r.status === 200 });
    sleep(0.1);
}
