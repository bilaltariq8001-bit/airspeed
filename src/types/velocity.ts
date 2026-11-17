export type SpeedUnit = 'KMH' | 'MPH' | 'KNOTS';
export type GaugeMode = 'analog' | 'digital';
export type TripStatus = 'idle' | 'recording' | 'paused';
export type ThemeMode = 'dark' | 'light';

export interface TrackSample {
  timestampMs: number;
  lat: number;
  lng: number;
  speedMps: number;
  altitudeM: number;
  accuracyM: number;
  bearingDeg: number;
}

export interface RecordedTrip {
  id: string;
  title: string;
  startTimeMs: number;
  endTimeMs: number;
  distanceMeters: number;
  maxSpeedMps: number;
  avgSpeedMps: number;
  durationMs: number;
  unitAtRecording: SpeedUnit;
  points: TrackSample[];
}

export const UNIT_CONFIG: Record<
  SpeedUnit,
  {
    label: string;
    distanceLabel: string;
    gaugeMax: number;
    toDisplay: (mps: number) => number;
    fromDisplay: (val: number) => number;
    formatDistance: (meters: number) => string;
  }
> = {
  KMH: {
    label: 'km/h',
    distanceLabel: 'km',
    gaugeMax: 240,
    toDisplay: (mps) => mps * 3.6,
    fromDisplay: (kmh) => kmh / 3.6,
    formatDistance: (m) => (m / 1000).toFixed(2),
  },
  MPH: {
    label: 'mph',
    distanceLabel: 'mi',
    gaugeMax: 160,
    toDisplay: (mps) => mps * 2.23693629,
    fromDisplay: (mph) => mph / 2.23693629,
    formatDistance: (m) => (m * 0.000621371).toFixed(2),
  },
  KNOTS: {
    label: 'knots',
    distanceLabel: 'nmi',
    gaugeMax: 130,
    toDisplay: (mps) => mps * 1.94384449,
    fromDisplay: (kn) => kn / 1.94384449,
    formatDistance: (m) => (m / 1852).toFixed(2),
  },
};

export function nextUnit(unit: SpeedUnit): SpeedUnit {
  if (unit === 'KMH') return 'MPH';
  if (unit === 'MPH') return 'KNOTS';
  return 'KMH';
}

export function bearingToCardinal(bearingDeg: number): string {
  const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const normalized = ((bearingDeg % 360) + 360) % 360;
  return dirs[Math.round(normalized / 45) % 8];
}

export function formatDuration(ms: number): string {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

export class KalmanSpeedSmoother {
  private estimate = 0;
  private errorCovariance = 1;
  private initialized = false;
  private readonly processNoise = 0.08;
  private readonly measurementNoise = 0.65;

  reset(): void {
    this.estimate = 0;
    this.errorCovariance = 1;
    this.initialized = false;
  }

  filter(measurementMps: number): number {
    const clamped = Math.max(0, measurementMps);
    if (!this.initialized) {
      this.estimate = clamped;
      this.errorCovariance = 1;
      this.initialized = true;
      return this.estimate;
    }
    const predictedCovariance = this.errorCovariance + this.processNoise;
    const kalmanGain = predictedCovariance / (predictedCovariance + this.measurementNoise);
    this.estimate = this.estimate + kalmanGain * (clamped - this.estimate);
    this.errorCovariance = (1 - kalmanGain) * predictedCovariance;
    return this.estimate < 0.25 ? 0 : this.estimate;
  }
}

export function exportTripToCsv(trip: RecordedTrip): string {
  const header = 'timestamp_iso,lat,lng,speed_mps,speed_kmh,altitude_m,accuracy_m,bearing_deg';
  const rows = trip.points.map((p) =>
    [
      new Date(p.timestampMs).toISOString(),
      p.lat.toFixed(6),
      p.lng.toFixed(6),
      p.speedMps.toFixed(2),
      (p.speedMps * 3.6).toFixed(2),
      p.altitudeM.toFixed(1),
      p.accuracyM.toFixed(1),
      p.bearingDeg.toFixed(1),
    ].join(',')
  );
  return [header, ...rows].join('\n');
}

export function exportTripToGpx(trip: RecordedTrip): string {
  const trkpts = trip.points
    .map(
      (p) =>
        `      <trkpt lat="${p.lat.toFixed(6)}" lon="${p.lng.toFixed(6)}">\n` +
        `        <ele>${p.altitudeM.toFixed(1)}</ele>\n` +
        `        <time>${new Date(p.timestampMs).toISOString()}</time>\n` +
        `        <extensions><speed>${p.speedMps.toFixed(2)}</speed></extensions>\n` +
        `      </trkpt>`
    )
    .join('\n');

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<gpx version="1.1" creator="Velocity Speedometer">',
    '  <trk>',
    `    <name>${trip.title.replace(/[<>&"]/g, '')}</name>`,
    '    <trkseg>',
    trkpts,
    '    </trkseg>',
    '  </trk>',
    '</gpx>',
  ].join('\n');
}

export function downloadTextFile(filename: string, content: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

function buildSamplePoints(
  baseTime: number,
  speedsKmh: number[],
  startLat: number,
  startLng: number
): TrackSample[] {
  let lat = startLat;
  let lng = startLng;
  return speedsKmh.map((kmh, idx) => {
    const speedMps = kmh / 3.6;
    const bearingDeg = (38 + Math.sin(idx * 0.45) * 24 + 360) % 360;
    const rad = (bearingDeg * Math.PI) / 180;
    lat += Math.cos(rad) * speedMps * 0.00018;
    lng += Math.sin(rad) * speedMps * 0.00021;
    return {
      timestampMs: baseTime + idx * 15000,
      lat,
      lng,
      speedMps,
      altitudeM: 42 + Math.sin(idx * 0.35) * 18,
      accuracyM: 4.5 + (idx % 3) * 1.2,
      bearingDeg,
    };
  });
}

const now = Date.now();

export const SAMPLE_TRIPS: RecordedTrip[] = [
  {
    id: 'trip-coastal-280',
    title: 'Coastal Express Run',
    startTimeMs: now - 1000 * 60 * 190,
    endTimeMs: now - 1000 * 60 * 168,
    distanceMeters: 28450,
    maxSpeedMps: 118 / 3.6,
    avgSpeedMps: 77.6 / 3.6,
    durationMs: 1000 * 60 * 22,
    unitAtRecording: 'KMH',
    points: buildSamplePoints(
      now - 1000 * 60 * 190,
      [0, 32, 58, 84, 96, 108, 118, 112, 104, 88, 72, 91, 106, 98, 64, 38, 12, 0],
      37.768,
      -122.492
    ),
  },
  {
    id: 'trip-ridge-loop',
    title: 'Skyline Ridge Climb',
    startTimeMs: now - 1000 * 60 * 60 * 26,
    endTimeMs: now - 1000 * 60 * 60 * 25 - 1000 * 60 * 44,
    distanceMeters: 14280,
    maxSpeedMps: 86 / 3.6,
    avgSpeedMps: 53.5 / 3.6,
    durationMs: 1000 * 60 * 16,
    unitAtRecording: 'KMH',
    points: buildSamplePoints(
      now - 1000 * 60 * 60 * 26,
      [0, 24, 44, 62, 74, 86, 79, 68, 54, 61, 76, 82, 69, 48, 29, 8, 0],
      37.442,
      -122.256
    ),
  },
];
