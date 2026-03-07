import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const VUS = __ENV.VUS || 50;
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: RAMP_TIME, target: VUS }, // Rampa de subida
        { duration: '1m', target: VUS },      // Mantenimiento de carga
        { duration: RAMP_TIME, target: 0 },   // Rampa de bajada
    ],
    thresholds: {
        http_req_duration: ['p(95)<70000'], // 95% de peticiones bajo 70s
        http_req_failed: ['rate<0.01'],     // Error rate menor a 1%
    },
};

export default function () {
    const url = `${BASE_URL}/api/requests`;
    const res = http.post(url);

    check(res, {
        'is status 200': (r) => r.status === 200,
        'has status COMPLETED': (r) => {
            try {
                return JSON.parse(r.body).status === 'COMPLETED';
            } catch (e) {
                return false;
            }
        },
    });

    sleep(1);
}

// Función especial de k6 para procesar los resultados al final de la prueba
export function handleSummary(data) {
    const httpReqs = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;
    const rps = data.metrics.http_reqs ? data.metrics.http_reqs.values.rate.toFixed(2) : 0;
    const p95 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 0;
    const errorRate = data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 0;

    let md = `# Reporte de Pruebas de Rendimiento

## Configuración de la Prueba
- **Usuarios Concurrentes (VUs)**: ${VUS}
- **Tiempo de Rampa**: ${RAMP_TIME}
- **URL Base**: ${BASE_URL}

## Resultados Principales
- **Total de Peticiones**: ${httpReqs}
- **RPS (Peticiones por segundo)**: ${rps}
- **Latencia P95**: ${p95} ms
- **Tasa de Error**: ${errorRate}%

## Validaciones (Checks)
`;

    if (data.root_group && data.root_group.checks) {
        data.root_group.checks.forEach(c => {
            const pass = c.passes;
            const fail = c.fails;
            const total = pass + fail;
            md += `- **${c.name}**: ${pass}/${total} exitosos\n`;
        });
    }

    md += `\n## Umbrales (Thresholds)\n`;
    for (const metricName in data.metrics) {
        if (data.metrics[metricName].thresholds) {
            md += `### ${metricName}\n`;
            for (const thresholdName in data.metrics[metricName].thresholds) {
                const ok = data.metrics[metricName].thresholds[thresholdName].ok;
                md += `- \`${thresholdName}\`: ${ok ? '✅ PASS' : '❌ FAIL'}\n`;
            }
        }
    }

    // Devolvemos el stdout normal y además un archivo summary.md
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'summary.md': md
    };
}
