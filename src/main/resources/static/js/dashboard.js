const API_BASE = '/api';
const WS_URL = '/traffic-ws';
const TOPIC_SIGNAL = '/topic/signal';

let stompClient = null;
let currentIntersectionId = null;
let currentLanes = [];
let chartInstance = null;

// UI Elements
const els = {
    intersectionSelect: document.getElementById('intersectionSelect'),
    totalVehicles: document.getElementById('totalVehicles'),
    avgCongestion: document.getElementById('avgCongestion'),
    activeIntersection: document.getElementById('activeIntersection'),
    wsStatus: document.getElementById('wsStatus'),
    wsStatusBadge: document.getElementById('wsStatusBadge'),
    aiPair: document.getElementById('aiPair'),
    aiLevel: document.getElementById('aiLevel'),
    aiGreen: document.getElementById('aiGreen'),
    aiRed: document.getElementById('aiRed'),
    aiTime: document.getElementById('aiTime'),
    signalCards: document.getElementById('signalCards'),
    heatmapGrid: document.getElementById('heatmapGrid'),
    countdownValue: document.getElementById('countdownValue'),
    roadV: document.getElementById('road-v'),
    roadH: document.getElementById('road-h')
};

// Utils
const $ = selector => document.querySelector(selector);
const setText = (el, text) => { if(el) el.textContent = text; };

async function fetchApi(url) {
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP error: ${res.status}`);
        return await res.json();
    } catch (e) {
        console.error(`Fetch failed for ${url}:`, e);
        return null;
    }
}

// 1. Load Intersections
async function loadIntersections() {
    const intersections = await fetchApi(`${API_BASE}/intersections`);
    if (!intersections || !intersections.length) return;

    els.intersectionSelect.innerHTML = intersections.map(i => `<option value="${i.id}">${i.name}</option>`).join('');
    
    currentIntersectionId = intersections[0].id;
    els.intersectionSelect.value = currentIntersectionId;
    setText(els.activeIntersection, intersections[0].name);

    els.intersectionSelect.addEventListener('change', (e) => {
        currentIntersectionId = e.target.value;
        const name = e.target.options[e.target.selectedIndex].text;
        setText(els.activeIntersection, name);
        onIntersectionChange();
    });

    await onIntersectionChange();
}

// 2. Load Lanes (when intersection changes)
async function onIntersectionChange() {
    await loadLanes(currentIntersectionId);
    await Promise.all([
        loadTrafficData(),
        loadSignalData()
    ]);
}

async function loadLanes(id) {
    const lanes = await fetchApi(`${API_BASE}/intersections/${id}/lanes`);
    currentLanes = lanes || [];
    renderSignalCards([]); // Reset cards structure
}

// 3. Load Traffic
async function loadTrafficData() {
    const traffic = await fetchApi(`${API_BASE}/traffic/latest`);
    if (!traffic) return;

    updateStatistics(traffic);
    renderHeatmap(traffic);
    updateVehicleCounts(traffic);
}

// 4. Load Signals
async function loadSignalData() {
    const signals = await fetchApi(`${API_BASE}/signals/latest`);
    if (!signals) return;
    renderSignalCards(signals);
}

// Renderers
function renderSignalCards(signals) {
    els.signalCards.innerHTML = currentLanes.map(lane => {
        const sig = signals.find(s => s.direction === lane.direction) || {};
        const state = sig.signal || 'RED';
        const colorClass = `signal-${state.toLowerCase()}`;
        const lightClass = `light-${state.toLowerCase()}`;
        
        return `
            <article class="signal-card ${colorClass}" id="card-${lane.direction}">
                <div class="card-top">
                    <span>${lane.direction}</span>
                    <span class="light ${lightClass}" id="light-${lane.direction}"></span>
                </div>
                <strong class="signal-state" id="state-${lane.direction}">${state}</strong>
                <div>Congestion: <b class="congestion" id="cong-${lane.direction}">${sig.congestionLevel || 0}%</b></div>
                <div>Vehicles: <b class="vehicles" id="veh-${lane.direction}">${sig.vehicleCount || 0}</b></div>
                <div>Timer: <b class="timer" id="time-${lane.direction}">${sig.remaining || 0}s</b></div>
            </article>
        `;
    }).join('');
}

function renderHeatmap(traffic) {
    els.heatmapGrid.innerHTML = traffic.map(t => {
        const c = t.congestionLevel || 0;
        let color = '#22c55e'; // GREEN 0-30
        if (c > 30 && c <= 60) color = '#f59e0b'; // YELLOW 31-60
        else if (c > 60) color = '#ef4444'; // RED 61-100

        return `
            <div class="heat-item">
                <div><strong>${t.direction}</strong><span style="float:right;color:#cbd5e1">${c}%</span></div>
                <div class="heat-bar"><div class="heat-fill" style="width:${c}%;background:${color}"></div></div>
            </div>
        `;
    }).join('');
}

function updateStatistics(traffic) {
    let total = 0, avg = 0;
    traffic.forEach(t => {
        total += t.vehicleCount || 0;
        avg += t.congestionLevel || 0;
    });
    if (traffic.length > 0) avg = Math.round(avg / traffic.length);

    setText(els.totalVehicles, total);
    setText(els.avgCongestion, `${avg}%`);
}

function updateVehicleCounts(traffic) {
    traffic.forEach(t => {
        const dir = t.direction.toLowerCase();
        const lbl = $(`.road-label.${dir} .vehicle-count`);
        if(lbl) lbl.textContent = t.vehicleCount || 0;
        
        const vehIcon = $(`#vehicle-${dir}`);
        if(vehIcon) {
            vehIcon.style.display = (t.vehicleCount || 0) > 0 ? 'block' : 'none';
        }
    });
}

function updateAiPanel(signals) {
    if(!signals || !signals.length) return;
    
    // Calculate highest congestion pair
    let highest = 0;
    let pairName = '';
    
    const pairs = [
        {name: 'North - South', dirs: ['North', 'South']},
        {name: 'East - West', dirs: ['East', 'West']}
    ];

    pairs.forEach(p => {
        let pairCongestion = 0;
        p.dirs.forEach(d => {
            const sig = signals.find(s => s.direction === d);
            if(sig) pairCongestion += (sig.congestionLevel || 0);
        });
        if(pairCongestion > highest) {
            highest = pairCongestion;
            pairName = p.name;
        }
    });

    const firstSig = signals[0];
    setText(els.aiPair, pairName || 'N/A');
    setText(els.aiLevel, firstSig.trafficLevel || 'N/A');
    setText(els.aiGreen, `${firstSig.greenDuration || 0} seconds`);
    setText(els.aiRed, `${firstSig.redDuration || 0} seconds`);
    setText(els.aiTime, new Date().toLocaleString());
}

// 5. WebSocket
function connectWebSocket() {
    updateWsStatus('CONNECTING', 'warning');
    
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        updateWsStatus('CONNECTED', 'success');
        stompClient.subscribe(TOPIC_SIGNAL, (msg) => {
            const data = JSON.parse(msg.body);
            handleSignalMessage(data);
        });
    }, (err) => {
        console.error('WS Error:', err);
        updateWsStatus('DISCONNECTED', 'danger');
        setTimeout(connectWebSocket, 5000); // 9. Auto reconnect 5s
    });
}

function updateWsStatus(status, colorClass) {
    if(els.wsStatusBadge) {
        els.wsStatusBadge.textContent = status;
        els.wsStatusBadge.className = `status-badge text-bg-${colorClass}`;
    }
    if(els.wsStatus) els.wsStatus.textContent = status;
}

// Handle incoming WS message
let currentSignals = [];
function handleSignalMessage(msg) {
    // Update local state
    const idx = currentSignals.findIndex(s => s.direction === msg.direction);
    if(idx > -1) currentSignals[idx] = msg;
    else currentSignals.push(msg);

    // Update UI Elements instantly
    updateSingleCard(msg);
    setText(els.countdownValue, msg.remaining);
    
    updateAiPanel(currentSignals);
    updateChart(msg);
    updateRoadGlow(msg);
}

function updateSingleCard(msg) {
    const card = document.getElementById(`card-${msg.direction}`);
    if(!card) return;

    const state = msg.signal || 'RED';
    card.className = `signal-card signal-${state.toLowerCase()}`;
    
    document.getElementById(`light-${msg.direction}`).className = `light light-${state.toLowerCase()}`;
    setText(document.getElementById(`state-${msg.direction}`), state);
    setText(document.getElementById(`cong-${msg.direction}`), `${msg.congestionLevel || 0}%`);
    setText(document.getElementById(`veh-${msg.direction}`), msg.vehicleCount || 0);
    setText(document.getElementById(`time-${msg.direction}`), `${msg.remaining || 0}s`);
}

function updateRoadGlow(msg) {
    if(msg.signal === 'GREEN') {
        if(['North', 'South'].includes(msg.direction)) {
            els.roadV.classList.add('road-green-glow');
            els.roadH.classList.remove('road-green-glow');
        } else {
            els.roadH.classList.add('road-green-glow');
            els.roadV.classList.remove('road-green-glow');
        }
    }
}

// 8. Chart.js
function initChart() {
    const ctx = document.getElementById('congestionChart').getContext('2d');
    chartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Avg Congestion %',
                data: [],
                borderColor: '#3b82f6',
                backgroundColor: 'rgba(59,130,246,0.1)',
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, max: 100 }
            }
        }
    });
}

function updateChart(msg) {
    if(!chartInstance) return;
    
    // Calculate avg congestion from currentSignals
    let total = 0;
    currentSignals.forEach(s => total += (s.congestionLevel || 0));
    const avg = currentSignals.length ? (total / currentSignals.length) : 0;

    const time = new Date().toLocaleTimeString();
    
    chartInstance.data.labels.push(time);
    chartInstance.data.datasets[0].data.push(avg);

    // Keep last 20
    if(chartInstance.data.labels.length > 20) {
        chartInstance.data.labels.shift();
        chartInstance.data.datasets[0].data.shift();
    }

    chartInstance.update();
}

// Init
document.addEventListener('DOMContentLoaded', () => {
    setInterval(() => setText(document.getElementById('currentTime'), new Date().toLocaleString()), 1000);
    initChart();
    loadIntersections().then(() => connectWebSocket());
});
