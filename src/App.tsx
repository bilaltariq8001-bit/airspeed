import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Download,
  Gauge,
  Moon,
  Navigation,
  Pause,
  Play,
  RotateCcw,
  Square,
  Sun,
  Trash2,
} from 'lucide-react';
import { AnalogGauge } from './components/AnalogGauge';
import { AndroidSpecPanel } from './components/AndroidSpecPanel';
import { HudOverlay } from './components/HudOverlay';
import { RoutePreview, SpeedChart } from './components/SpeedChart';
import {
  GaugeMode,
  KalmanSpeedSmoother,
  RecordedTrip,
  SAMPLE_TRIPS,
  SpeedUnit,
  ThemeMode,
  TrackSample,
  TripStatus,
  UNIT_CONFIG,
  bearingToCardinal,
  downloadTextFile,
  exportTripToCsv,
  exportTripToGpx,
  formatDuration,
  nextUnit,
} from './types/velocity';

type ActiveSection = 'dashboard' | 'history' | 'android';

const STORAGE_ODOMETER_KEY = 'velocity_odometer_meters_v1';
const STORAGE_TRIPS_KEY = 'velocity_saved_trips_v1';

export function App() {
  const [theme, setTheme] = useState<ThemeMode>('dark');
  const [activeSection, setActiveSection] = useState<ActiveSection>('dashboard');
  const [hudOpen, setHudOpen] = useState(false);

  const [unit, setUnit] = useState<SpeedUnit>('KMH');
  const [gaugeMode, setGaugeMode] = useState<GaugeMode>('analog');
  const [speedLimitEnabled, setSpeedLimitEnabled] = useState(true);
  const [speedLimitKmh, setSpeedLimitKmh] = useState(100);
  const [kalmanEnabled, setKalmanEnabled] = useState(true);

  // Telemetry state (internal units: m/s, meters, degrees)
  const [targetSimKmh, setTargetSimKmh] = useState(74);
  const [useBrowserGps, setUseBrowserGps] = useState(false);
  const [gpsError, setGpsError] = useState<string | null>(null);
  const [currentSpeedMps, setCurrentSpeedMps] = useState(74 / 3.6);
  const [accuracyMeters, setAccuracyMeters] = useState(5.2);
  const [altitudeMeters, setAltitudeMeters] = useState(48);
  const [bearingDeg, setBearingDeg] = useState(42);

  // Active trip state
  const [tripStatus, setTripStatus] = useState<TripStatus>('idle');
  const [tripDistanceMeters, setTripDistanceMeters] = useState(0);
  const [tripDurationMs, setTripDurationMs] = useState(0);
  const [tripMaxSpeedMps, setTripMaxSpeedMps] = useState(0);
  const [tripPoints, setTripPoints] = useState<TrackSample[]>([]);
  const tripStartRef = useRef<number>(Date.now());
  const coordRef = useRef<{ lat: number; lng: number }>({ lat: 37.7749, lng: -122.4194 });
  const smootherRef = useRef(new KalmanSpeedSmoother());

  // Persistent odometer & trips
  const [odometerMeters, setOdometerMeters] = useState<number>(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_ODOMETER_KEY);
      return saved ? Number(saved) || 42730 : 42730;
    } catch {
      return 42730;
    }
  });

  const [trips, setTrips] = useState<RecordedTrip[]>(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_TRIPS_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as RecordedTrip[];
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // ignore storage errors
    }
    return SAMPLE_TRIPS;
  });

  const [selectedTripId, setSelectedTripId] = useState<string>(trips[0]?.id ?? '');
  const [confirmClearAll, setConfirmClearAll] = useState(false);
  const [confirmResetOdo, setConfirmResetOdo] = useState(false);

  // Sync Tailwind dark class on <html>
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [theme]);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_ODOMETER_KEY, String(Math.round(odometerMeters)));
    } catch {
      // ignore storage errors
    }
  }, [odometerMeters]);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_TRIPS_KEY, JSON.stringify(trips));
    } catch {
      // ignore storage errors
    }
  }, [trips]);

  // Browser GPS watcher when explicitly enabled by user
  useEffect(() => {
    if (!useBrowserGps) {
      setGpsError(null);
      return;
    }
    if (!('geolocation' in navigator)) {
      setGpsError('Browser geolocation is not available in this environment.');
      setUseBrowserGps(false);
      return;
    }

    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        setGpsError(null);
        const acc = pos.coords.accuracy ?? 25;
        setAccuracyMeters(acc);
        if (acc > 30) return;

        const rawMps = Math.max(0, pos.coords.speed ?? 0);
        const smoothed = kalmanEnabled ? smootherRef.current.filter(rawMps) : rawMps;
        setCurrentSpeedMps(smoothed);
        if (pos.coords.altitude !== null) setAltitudeMeters(pos.coords.altitude);
        if (pos.coords.heading !== null && !Number.isNaN(pos.coords.heading)) {
          setBearingDeg(pos.coords.heading);
        }
        coordRef.current = { lat: pos.coords.latitude, lng: pos.coords.longitude };
      },
      (err) => {
        setGpsError(err.message || 'Location access denied; using simulated telemetry.');
        setUseBrowserGps(false);
      },
      { enableHighAccuracy: true, maximumAge: 500, timeout: 8000 }
    );

    return () => navigator.geolocation.clearWatch(watchId);
  }, [kalmanEnabled, useBrowserGps]);

  // 500 ms telemetry loop matching Android FusedLocationSource interval
  useEffect(() => {
    if (useBrowserGps) return;
    const timer = window.setInterval(() => {
      const baseMps = targetSimKmh / 3.6;
      const jitter = baseMps > 0.5 ? (Math.sin(Date.now() / 650) * 0.45 + (Math.random() - 0.5) * 0.6) : 0;
      const rawMps = Math.max(0, baseMps + jitter);
      const nextMps = kalmanEnabled ? smootherRef.current.filter(rawMps) : rawMps;

      setCurrentSpeedMps(nextMps);
      setAccuracyMeters(4.2 + Math.abs(Math.sin(Date.now() / 2400)) * 3.8);
      setAltitudeMeters((prev) => Number((prev + Math.sin(Date.now() / 3000) * 0.15).toFixed(1)));
      if (nextMps > 0.5) {
        setBearingDeg((prev) => (prev + Math.sin(Date.now() / 4200) * 1.4 + 360) % 360);
      }
    }, 500);

    return () => window.clearInterval(timer);
  }, [kalmanEnabled, targetSimKmh, useBrowserGps]);

  // Accumulate trip stats while recording
  useEffect(() => {
    if (tripStatus !== 'recording') return;

    const timer = window.setInterval(() => {
      const dtSec = 0.5;
      const deltaMeters = currentSpeedMps * dtSec;
      const rad = (bearingDeg * Math.PI) / 180;
      coordRef.current = {
        lat: coordRef.current.lat + Math.cos(rad) * deltaMeters * 0.000009,
        lng: coordRef.current.lng + Math.sin(rad) * deltaMeters * 0.000011,
      };

      setTripDurationMs((prev) => prev + 500);
      setTripDistanceMeters((prev) => prev + deltaMeters);
      setOdometerMeters((prev) => prev + deltaMeters);
      setTripMaxSpeedMps((prev) => Math.max(prev, currentSpeedMps));
      setTripPoints((prev) => [
        ...prev.slice(-240),
        {
          timestampMs: Date.now(),
          lat: coordRef.current.lat,
          lng: coordRef.current.lng,
          speedMps: currentSpeedMps,
          altitudeM: altitudeMeters,
          accuracyM: accuracyMeters,
          bearingDeg,
        },
      ]);
    }, 500);

    return () => window.clearInterval(timer);
  }, [accuracyMeters, altitudeMeters, bearingDeg, currentSpeedMps, tripStatus]);

  const unitCfg = UNIT_CONFIG[unit];
  const speedLimitMps = speedLimitKmh / 3.6;
  const isOverLimit = speedLimitEnabled && currentSpeedMps > speedLimitMps;
  const isNearLimit = !isOverLimit && speedLimitEnabled && currentSpeedMps >= speedLimitMps * 0.9;
  const avgSpeedMps = tripDurationMs > 0 ? tripDistanceMeters / (tripDurationMs / 1000) : 0;

  const gpsStatus = useMemo(() => {
    if (accuracyMeters < 10) {
      return {
        label: `GPS Locked · ±${accuracyMeters.toFixed(1)} m`,
        dotClass: 'bg-[#00E676]',
        textClass: 'text-[#00B248] dark:text-[#00E676]',
      };
    }
    if (accuracyMeters <= 30) {
      return {
        label: `GPS Fair · ±${accuracyMeters.toFixed(1)} m`,
        dotClass: 'bg-[#FFB300]',
        textClass: 'text-[#B27B00] dark:text-[#FFB300]',
      };
    }
    return {
      label: `GPS Weak · ±${accuracyMeters.toFixed(1)} m`,
      dotClass: 'bg-[#FF1744]',
      textClass: 'text-[#FF1744]',
    };
  }, [accuracyMeters]);

  const selectedTrip = useMemo(
    () => trips.find((t) => t.id === selectedTripId) ?? trips[0] ?? null,
    [selectedTripId, trips]
  );

  const handleStartOrToggleTrip = () => {
    if (tripStatus === 'idle') {
      tripStartRef.current = Date.now();
      setTripDistanceMeters(0);
      setTripDurationMs(0);
      setTripMaxSpeedMps(currentSpeedMps);
      setTripPoints([
        {
          timestampMs: Date.now(),
          lat: coordRef.current.lat,
          lng: coordRef.current.lng,
          speedMps: currentSpeedMps,
          altitudeM: altitudeMeters,
          accuracyM: accuracyMeters,
          bearingDeg,
        },
      ]);
      setTripStatus('recording');
      setActiveSection('dashboard');
      return;
    }
    if (tripStatus === 'recording') {
      setTripStatus('paused');
    } else {
      setTripStatus('recording');
    }
  };

  const handleStopAndSaveTrip = () => {
    if (tripStatus === 'idle') return;
    const endTime = Date.now();
    const newTrip: RecordedTrip = {
      id: `trip-${endTime}`,
      title: `Drive ${new Date(tripStartRef.current).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
      })}`,
      startTimeMs: tripStartRef.current,
      endTimeMs: endTime,
      distanceMeters: tripDistanceMeters,
      maxSpeedMps: Math.max(tripMaxSpeedMps, currentSpeedMps),
      avgSpeedMps,
      durationMs: Math.max(1000, tripDurationMs),
      unitAtRecording: unit,
      points:
        tripPoints.length >= 2
          ? tripPoints
          : [
              {
                timestampMs: tripStartRef.current,
                lat: coordRef.current.lat,
                lng: coordRef.current.lng,
                speedMps: currentSpeedMps,
                altitudeM: altitudeMeters,
                accuracyM: accuracyMeters,
                bearingDeg,
              },
              {
                timestampMs: endTime,
                lat: coordRef.current.lat + 0.0005,
                lng: coordRef.current.lng + 0.0005,
                speedMps: currentSpeedMps,
                altitudeM: altitudeMeters,
                accuracyM: accuracyMeters,
                bearingDeg,
              },
            ],
    };

    setTrips((prev) => [newTrip, ...prev]);
    setSelectedTripId(newTrip.id);
    setTripStatus('idle');
    setTripDistanceMeters(0);
    setTripDurationMs(0);
    setTripMaxSpeedMps(0);
    setTripPoints([]);
  };

  const handleDeleteTrip = (id: string) => {
    setTrips((prev) => {
      const next = prev.filter((t) => t.id !== id);
      if (selectedTripId === id) {
        setSelectedTripId(next[0]?.id ?? '');
      }
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA] text-[#0B0F14] transition-colors dark:bg-[#0B0F14] dark:text-white">
      {hudOpen && (
        <HudOverlay
          speedMps={currentSpeedMps}
          unit={unit}
          bearingDeg={bearingDeg}
          speedLimitEnabled={speedLimitEnabled}
          speedLimitMps={speedLimitMps}
          onCycleUnit={() => setUnit((u) => nextUnit(u))}
          onExit={() => setHudOpen(false)}
        />
      )}

      {/* Three-Zone Top Bar Contract */}
      <header className="sticky top-0 z-30 border-b border-[#DCE3EC] bg-[#F5F7FA]/90 backdrop-blur-md dark:border-[#2A3340] dark:bg-[#0B0F14]/90">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-8 px-4 py-3.5 sm:px-6">
          <button
            type="button"
            onClick={() => setActiveSection('dashboard')}
            className="font-speed text-lg font-bold tracking-wider whitespace-nowrap shrink-0 text-[#0B0F14] focus-visible:outline-2 focus-visible:outline-[#00E5FF] dark:text-white"
          >
            VELOCITY
          </button>

          <nav className="flex items-center gap-6 overflow-x-auto text-sm font-medium">
            <button
              type="button"
              onClick={() => setActiveSection('dashboard')}
              className={`min-h-[40px] whitespace-nowrap shrink-0 transition-colors ${
                activeSection === 'dashboard'
                  ? 'text-[#0091EA] underline underline-offset-8 dark:text-[#00E5FF]'
                  : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
              }`}
            >
              Speedometer
            </button>
            <button
              type="button"
              onClick={() => setHudOpen(true)}
              className="min-h-[40px] whitespace-nowrap shrink-0 text-[#5A6573] transition-colors hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white"
            >
              HUD Mode
            </button>
            <button
              type="button"
              onClick={() => setActiveSection('history')}
              className={`min-h-[40px] whitespace-nowrap shrink-0 transition-colors ${
                activeSection === 'history'
                  ? 'text-[#0091EA] underline underline-offset-8 dark:text-[#00E5FF]'
                  : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
              }`}
            >
              Trip History
            </button>
            <button
              type="button"
              onClick={() => setActiveSection('android')}
              className={`min-h-[40px] whitespace-nowrap shrink-0 transition-colors ${
                activeSection === 'android'
                  ? 'text-[#0091EA] underline underline-offset-8 dark:text-[#00E5FF]'
                  : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
              }`}
            >
              Android App
            </button>
          </nav>

          <div className="shrink-0">
            <button
              type="button"
              onClick={handleStartOrToggleTrip}
              className={`inline-flex min-h-[44px] items-center gap-2 rounded-[28px] px-5 py-2 text-xs font-semibold whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-[#00E5FF] ${
                tripStatus === 'recording'
                  ? 'bg-[#FFB300] text-[#0B0F14] hover:bg-[#FFC107]'
                  : 'bg-[#0091EA] text-white hover:bg-[#0284C7] dark:bg-[#00E5FF] dark:text-[#0B0F14] dark:hover:bg-[#33ECFF]'
              }`}
            >
              {tripStatus === 'recording' ? (
                <>
                  <Pause className="h-4 w-4" aria-hidden="true" />
                  Pause Trip
                </>
              ) : tripStatus === 'paused' ? (
                <>
                  <Play className="h-4 w-4" aria-hidden="true" />
                  Resume Trip
                </>
              ) : (
                <>
                  <Play className="h-4 w-4" aria-hidden="true" />
                  Start Trip
                </>
              )}
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
        {/* Workspace Toolbar below Header */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4 border-b border-[#DCE3EC] pb-4 dark:border-[#2A3340]">
          <div className="flex flex-wrap items-center gap-4 text-xs font-medium">
            <span className={`inline-flex items-center gap-2 ${gpsStatus.textClass}`}>
              <span className={`h-2 w-2 rounded-full ${gpsStatus.dotClass}`} aria-hidden="true" />
              {gpsStatus.label}
            </span>
            <span aria-hidden="true" className="text-[#9AA4B2]">
              ·
            </span>
            <span className="font-mono-tabular text-[#5A6573] dark:text-[#9AA4B2]">
              Odometer: {unitCfg.formatDistance(odometerMeters)} {unitCfg.distanceLabel}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Gauge mode segmented switch */}
            <div className="flex items-center rounded-xl border border-[#DCE3EC] bg-white p-1 dark:border-[#2A3340] dark:bg-[#141A22]">
              <button
                type="button"
                onClick={() => setGaugeMode('analog')}
                className={`min-h-[36px] rounded-lg px-3 py-1.5 text-xs font-medium whitespace-nowrap transition-colors ${
                  gaugeMode === 'analog'
                    ? 'bg-[#0091EA] text-white dark:bg-[#00E5FF] dark:text-[#0B0F14]'
                    : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
                }`}
              >
                Analog Gauge
              </button>
              <button
                type="button"
                onClick={() => setGaugeMode('digital')}
                className={`min-h-[36px] rounded-lg px-3 py-1.5 text-xs font-medium whitespace-nowrap transition-colors ${
                  gaugeMode === 'digital'
                    ? 'bg-[#0091EA] text-white dark:bg-[#00E5FF] dark:text-[#0B0F14]'
                    : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
                }`}
              >
                Digital
              </button>
            </div>

            {/* Unit segmented control */}
            <div className="flex items-center rounded-xl border border-[#DCE3EC] bg-white p-1 dark:border-[#2A3340] dark:bg-[#141A22]">
              {(['KMH', 'MPH', 'KNOTS'] as SpeedUnit[]).map((u) => (
                <button
                  key={u}
                  type="button"
                  onClick={() => setUnit(u)}
                  className={`min-h-[36px] rounded-lg px-3 py-1.5 text-xs font-medium whitespace-nowrap transition-colors ${
                    unit === u
                      ? 'bg-[#651FFF] text-white dark:bg-[#7C4DFF]'
                      : 'text-[#5A6573] hover:text-[#0B0F14] dark:text-[#9AA4B2] dark:hover:text-white'
                  }`}
                >
                  {UNIT_CONFIG[u].label}
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}
              aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
              className="inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl border border-[#DCE3EC] bg-white text-[#5A6573] transition-colors hover:text-[#0B0F14] dark:border-[#2A3340] dark:bg-[#141A22] dark:text-[#9AA4B2] dark:hover:text-white"
            >
              {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
          </div>
        </div>

        {activeSection === 'dashboard' && (
          <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
            {/* Dominant Speedometer Stage */}
            <section className="rounded-[20px] border border-[#DCE3EC] bg-white p-6 dark:border-[#2A3340] dark:bg-[#141A22] lg:col-span-7">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h1 className="text-lg font-semibold text-[#0B0F14] dark:text-white">
                  {gaugeMode === 'analog' ? 'Analog Speedometer' : 'Digital Telemetry'}
                </h1>
                <span
                  className={`text-xs font-medium ${
                    isOverLimit
                      ? 'text-[#FF1744]'
                      : isNearLimit
                        ? 'text-[#B27B00] dark:text-[#FFB300]'
                        : 'text-[#5A6573] dark:text-[#9AA4B2]'
                  }`}
                >
                  {isOverLimit
                    ? `Over Speed Limit (${Math.round(unitCfg.toDisplay(speedLimitMps))} ${unitCfg.label})`
                    : speedLimitEnabled
                      ? `Limit ${Math.round(unitCfg.toDisplay(speedLimitMps))} ${unitCfg.label}`
                      : 'Speed Limit Off'}
                </span>
              </div>

              <div className="my-6">
                {gaugeMode === 'analog' ? (
                  <AnalogGauge
                    speedMps={currentSpeedMps}
                    maxSpeedMps={tripMaxSpeedMps}
                    unit={unit}
                    speedLimitEnabled={speedLimitEnabled}
                    speedLimitMps={speedLimitMps}
                    bearingDeg={bearingDeg}
                    theme={theme}
                    onCycleUnit={() => setUnit((u) => nextUnit(u))}
                  />
                ) : (
                  <div className="flex flex-col items-center py-8">
                    <div
                      className={`font-speed text-7xl font-bold tracking-tight sm:text-8xl ${
                        isOverLimit
                          ? 'text-[#FF1744] motion-safe:animate-pulse'
                          : isNearLimit
                            ? 'text-[#FFB300]'
                            : 'text-[#0B0F14] dark:text-white'
                      }`}
                    >
                      {Math.round(unitCfg.toDisplay(currentSpeedMps))}
                    </div>
                    <button
                      type="button"
                      onClick={() => setUnit((u) => nextUnit(u))}
                      className="mt-2 min-h-[44px] px-4 text-sm font-medium tracking-[0.1em] uppercase text-[#5A6573] hover:text-[#0091EA] dark:text-[#9AA4B2] dark:hover:text-[#00E5FF]"
                    >
                      {unitCfg.label} · {bearingToCardinal(bearingDeg)} ({Math.round(bearingDeg)}°)
                    </button>
                    <div className="mt-6 h-3 w-full max-w-md overflow-hidden rounded-full bg-[#DCE3EC] dark:bg-[#2A3340]">
                      <div
                        className="h-full rounded-full transition-all duration-200"
                        style={{
                          width: `${Math.min(
                            100,
                            (unitCfg.toDisplay(currentSpeedMps) / unitCfg.gaugeMax) * 100
                          )}%`,
                          background: isOverLimit
                            ? '#FF1744'
                            : 'linear-gradient(90deg, #00E676 0%, #00E5FF 48%, #FFB300 80%, #FF1744 100%)',
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Trip Control Bar */}
              <div className="flex flex-wrap items-center justify-center gap-3 border-t border-[#DCE3EC] pt-5 dark:border-[#2A3340]">
                <button
                  type="button"
                  onClick={handleStartOrToggleTrip}
                  className="inline-flex min-h-[48px] items-center gap-2 rounded-[28px] bg-[#0091EA] px-6 py-2.5 text-sm font-semibold whitespace-nowrap text-white transition-colors hover:bg-[#0284C7] dark:bg-[#00E5FF] dark:text-[#0B0F14] dark:hover:bg-[#33ECFF]"
                >
                  {tripStatus === 'recording' ? (
                    <>
                      <Pause className="h-4 w-4" aria-hidden="true" />
                      Pause
                    </>
                  ) : tripStatus === 'paused' ? (
                    <>
                      <Play className="h-4 w-4" aria-hidden="true" />
                      Resume
                    </>
                  ) : (
                    <>
                      <Gauge className="h-4 w-4" aria-hidden="true" />
                      Start Recording
                    </>
                  )}
                </button>

                {tripStatus !== 'idle' && (
                  <button
                    type="button"
                    onClick={handleStopAndSaveTrip}
                    className="inline-flex min-h-[48px] items-center gap-2 rounded-[28px] bg-[#FF1744] px-6 py-2.5 text-sm font-semibold whitespace-nowrap text-white transition-colors hover:bg-[#E11D48]"
                  >
                    <Square className="h-4 w-4" aria-hidden="true" />
                    Stop & Save Trip
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => setHudOpen(true)}
                  className="inline-flex min-h-[48px] items-center gap-2 rounded-[28px] border border-[#DCE3EC] bg-[#F5F7FA] px-5 py-2.5 text-sm font-medium whitespace-nowrap text-[#0B0F14] transition-colors hover:border-[#0091EA] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-white dark:hover:border-[#00E5FF]"
                >
                  Launch HUD
                </button>
              </div>
            </section>

            {/* Telemetry Metrics & Simulation Controls */}
            <aside className="space-y-6 lg:col-span-5">
              {/* 6 Telemetry Stat Cards */}
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-2">
                {[
                  {
                    label: 'Max Speed',
                    value: `${Math.round(unitCfg.toDisplay(tripMaxSpeedMps))} ${unitCfg.label}`,
                    accent: 'text-[#651FFF] dark:text-[#7C4DFF]',
                  },
                  {
                    label: 'Avg Speed',
                    value: `${Math.round(unitCfg.toDisplay(avgSpeedMps))} ${unitCfg.label}`,
                    accent: 'text-[#0B0F14] dark:text-white',
                  },
                  {
                    label: 'Trip Distance',
                    value: `${unitCfg.formatDistance(tripDistanceMeters)} ${unitCfg.distanceLabel}`,
                    accent: 'text-[#0091EA] dark:text-[#00E5FF]',
                  },
                  {
                    label: 'Duration',
                    value: formatDuration(tripDurationMs),
                    accent: 'text-[#0B0F14] dark:text-white',
                  },
                  {
                    label: 'Altitude',
                    value: `${Math.round(altitudeMeters)} m`,
                    accent: 'text-[#0B0F14] dark:text-white',
                  },
                  {
                    label: 'Heading',
                    value: `${bearingToCardinal(bearingDeg)} · ${Math.round(bearingDeg)}°`,
                    accent: 'text-[#0B0F14] dark:text-white',
                  },
                ].map((stat) => (
                  <div
                    key={stat.label}
                    className="rounded-[20px] border border-[#DCE3EC] bg-white p-4 dark:border-[#2A3340] dark:bg-[#141A22]"
                  >
                    <div className="text-xs font-medium tracking-[0.06em] uppercase text-[#5A6573] dark:text-[#9AA4B2]">
                      {stat.label}
                    </div>
                    <div className={`mt-1.5 font-mono-tabular text-xl font-semibold ${stat.accent}`}>
                      {stat.value}
                    </div>
                  </div>
                ))}
              </div>

              {/* Interactive Signal & Calibration Controls */}
              <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-5 dark:border-[#2A3340] dark:bg-[#141A22]">
                <div className="flex items-center justify-between gap-2">
                  <h2 className="text-base font-semibold text-[#0B0F14] dark:text-white">
                    Telemetry Source & Limits
                  </h2>
                  <button
                    type="button"
                    onClick={() => setUseBrowserGps((v) => !v)}
                    className={`inline-flex min-h-[36px] items-center gap-1.5 rounded-lg border px-3 py-1 text-xs font-medium whitespace-nowrap transition-colors ${
                      useBrowserGps
                        ? 'border-[#00E676] bg-[#00E676]/15 text-[#00B248] dark:text-[#00E676]'
                        : 'border-[#DCE3EC] bg-[#F5F7FA] text-[#5A6573] hover:text-[#0B0F14] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-[#9AA4B2] dark:hover:text-white'
                    }`}
                  >
                    <Navigation className="h-3.5 w-3.5" aria-hidden="true" />
                    {useBrowserGps ? 'Live GPS Active' : 'Use Browser GPS'}
                  </button>
                </div>

                {gpsError && (
                  <p className="mt-2 text-xs text-[#FF1744]">{gpsError}</p>
                )}

                {!useBrowserGps && (
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-xs">
                      <label htmlFor="sim-speed" className="font-medium text-[#5A6573] dark:text-[#9AA4B2]">
                        Simulated GPS Speed
                      </label>
                      <span className="font-mono-tabular font-semibold text-[#0091EA] dark:text-[#00E5FF]">
                        {Math.round(unitCfg.toDisplay(targetSimKmh / 3.6))} {unitCfg.label}
                      </span>
                    </div>
                    <input
                      id="sim-speed"
                      type="range"
                      min={0}
                      max={220}
                      value={targetSimKmh}
                      onChange={(e) => setTargetSimKmh(Number(e.target.value))}
                      className="mt-2 w-full accent-[#00E5FF]"
                    />
                    <div className="mt-2 flex flex-wrap gap-2">
                      {[
                        { label: 'Stop (0)', kmh: 0 },
                        { label: 'City (50)', kmh: 50 },
                        { label: 'Highway (105)', kmh: 105 },
                        { label: 'Over Limit (140)', kmh: 140 },
                      ].map((preset) => (
                        <button
                          key={preset.label}
                          type="button"
                          onClick={() => setTargetSimKmh(preset.kmh)}
                          className="min-h-[36px] rounded-lg border border-[#DCE3EC] bg-[#F5F7FA] px-3 py-1 text-xs font-medium text-[#0B0F14] transition-colors hover:border-[#0091EA] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-white dark:hover:border-[#00E5FF]"
                        >
                          {preset.label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                <div className="mt-5 space-y-3 border-t border-[#DCE3EC] pt-4 dark:border-[#2A3340]">
                  <div className="flex items-center justify-between gap-3">
                    <label htmlFor="limit-range" className="text-xs font-medium text-[#5A6573] dark:text-[#9AA4B2]">
                      Speed Limit Alert ({Math.round(unitCfg.toDisplay(speedLimitKmh / 3.6))} {unitCfg.label})
                    </label>
                    <button
                      type="button"
                      onClick={() => setSpeedLimitEnabled((v) => !v)}
                      className={`min-h-[34px] rounded-lg px-3 py-1 text-xs font-semibold ${
                        speedLimitEnabled
                          ? 'bg-[#FF1744]/15 text-[#FF1744]'
                          : 'bg-[#F5F7FA] text-[#5A6573] dark:bg-[#1E2630] dark:text-[#9AA4B2]'
                      }`}
                    >
                      {speedLimitEnabled ? 'Enabled' : 'Disabled'}
                    </button>
                  </div>
                  {speedLimitEnabled && (
                    <input
                      id="limit-range"
                      type="range"
                      min={30}
                      max={180}
                      step={5}
                      value={speedLimitKmh}
                      onChange={(e) => setSpeedLimitKmh(Number(e.target.value))}
                      className="w-full accent-[#FF1744]"
                    />
                  )}

                  <div className="flex items-center justify-between gap-3 pt-1">
                    <span className="text-xs font-medium text-[#5A6573] dark:text-[#9AA4B2]">
                      1D Kalman Speed Smoothing
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        smootherRef.current.reset();
                        setKalmanEnabled((v) => !v);
                      }}
                      className={`min-h-[34px] rounded-lg px-3 py-1 text-xs font-semibold ${
                        kalmanEnabled
                          ? 'bg-[#0091EA]/15 text-[#0091EA] dark:bg-[#00E5FF]/15 dark:text-[#00E5FF]'
                          : 'bg-[#F5F7FA] text-[#5A6573] dark:bg-[#1E2630] dark:text-[#9AA4B2]'
                      }`}
                    >
                      {kalmanEnabled ? 'Active' : 'Raw GPS'}
                    </button>
                  </div>

                  <div className="flex items-center justify-between gap-3 pt-1">
                    <span className="text-xs font-medium text-[#5A6573] dark:text-[#9AA4B2]">
                      Lifetime Odometer
                    </span>
                    {confirmResetOdo ? (
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setOdometerMeters(0);
                            setConfirmResetOdo(false);
                          }}
                          className="min-h-[34px] rounded-lg bg-[#FF1744] px-3 py-1 text-xs font-semibold text-white"
                        >
                          Confirm Reset
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmResetOdo(false)}
                          className="min-h-[34px] rounded-lg border border-[#DCE3EC] px-2.5 py-1 text-xs text-[#5A6573] dark:border-[#2A3340] dark:text-[#9AA4B2]"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setConfirmResetOdo(true)}
                        className="inline-flex min-h-[34px] items-center gap-1.5 rounded-lg border border-[#DCE3EC] px-3 py-1 text-xs font-medium text-[#5A6573] hover:text-[#0B0F14] dark:border-[#2A3340] dark:text-[#9AA4B2] dark:hover:text-white"
                      >
                        <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                        Reset
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </aside>
          </div>
        )}

        {activeSection === 'history' && (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
            {/* Trip List */}
            <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-5 dark:border-[#2A3340] dark:bg-[#141A22] lg:col-span-5">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-[#0B0F14] dark:text-white">
                  Saved Trips ({trips.length})
                </h2>
                {trips.length > 0 && (
                  confirmClearAll ? (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setTrips([]);
                          setSelectedTripId('');
                          setConfirmClearAll(false);
                        }}
                        className="min-h-[36px] rounded-lg bg-[#FF1744] px-3 py-1 text-xs font-semibold text-white"
                      >
                        Delete All
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmClearAll(false)}
                        className="min-h-[36px] rounded-lg border border-[#DCE3EC] px-2.5 py-1 text-xs text-[#5A6573] dark:border-[#2A3340] dark:text-[#9AA4B2]"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmClearAll(true)}
                      className="inline-flex min-h-[36px] items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium text-[#FF1744] hover:bg-[#FF1744]/10"
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      Clear All
                    </button>
                  )
                )}
              </div>

              {trips.length === 0 ? (
                <div className="mt-6 rounded-2xl border border-[#DCE3EC] bg-[#F5F7FA] p-6 text-center text-sm text-[#5A6573] dark:border-[#2A3340] dark:bg-[#0B0F14] dark:text-[#9AA4B2]">
                  No recorded trips yet. Start a trip from the Speedometer tab and click Stop & Save.
                </div>
              ) : (
                <div className="mt-4 divide-y divide-[#DCE3EC] dark:divide-[#2A3340]">
                  {trips.map((trip) => {
                    const isSelected = selectedTrip?.id === trip.id;
                    return (
                      <div
                        key={trip.id}
                        className={`flex items-center justify-between gap-3 py-3.5 transition-colors ${
                          isSelected ? 'bg-[#0091EA]/5 dark:bg-[#00E5FF]/5 rounded-xl px-3 -mx-3' : ''
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() => setSelectedTripId(trip.id)}
                          className="min-w-0 flex-1 text-left"
                        >
                          <div className="truncate text-sm font-semibold text-[#0B0F14] dark:text-white">
                            {trip.title}
                          </div>
                          <div className="mt-1 font-mono-tabular text-xs text-[#5A6573] dark:text-[#9AA4B2]">
                            {unitCfg.formatDistance(trip.distanceMeters)} {unitCfg.distanceLabel} ·{' '}
                            {formatDuration(trip.durationMs)} · Max{' '}
                            {Math.round(unitCfg.toDisplay(trip.maxSpeedMps))} {unitCfg.label}
                          </div>
                        </button>

                        <button
                          type="button"
                          onClick={() => handleDeleteTrip(trip.id)}
                          aria-label={`Delete ${trip.title}`}
                          className="inline-flex min-h-[40px] min-w-[40px] shrink-0 items-center justify-center rounded-lg text-[#5A6573] transition-colors hover:bg-[#FF1744]/10 hover:text-[#FF1744] dark:text-[#9AA4B2]"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Trip Detail & Export */}
            <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-6 dark:border-[#2A3340] dark:bg-[#141A22] lg:col-span-7">
              {selectedTrip ? (
                <div className="space-y-6">
                  <div className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                      <h2 className="text-xl font-semibold text-[#0B0F14] dark:text-white">
                        {selectedTrip.title}
                      </h2>
                      <p className="mt-0.5 text-xs text-[#5A6573] dark:text-[#9AA4B2]">
                        {new Date(selectedTrip.startTimeMs).toLocaleString()}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() =>
                          downloadTextFile(
                            `${selectedTrip.id}.gpx`,
                            exportTripToGpx(selectedTrip),
                            'application/gpx+xml'
                          )
                        }
                        className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] px-3.5 py-2 text-xs font-medium text-[#0B0F14] transition-colors hover:border-[#0091EA] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-white dark:hover:border-[#00E5FF]"
                      >
                        <Download className="h-3.5 w-3.5" aria-hidden="true" />
                        Export GPX
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          downloadTextFile(
                            `${selectedTrip.id}.csv`,
                            exportTripToCsv(selectedTrip),
                            'text/csv'
                          )
                        }
                        className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] px-3.5 py-2 text-xs font-medium text-[#0B0F14] transition-colors hover:border-[#0091EA] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-white dark:hover:border-[#00E5FF]"
                      >
                        <Download className="h-3.5 w-3.5" aria-hidden="true" />
                        Export CSV
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <div className="rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] p-3 dark:border-[#2A3340] dark:bg-[#1E2630]">
                      <div className="text-[11px] uppercase text-[#5A6573] dark:text-[#9AA4B2]">Distance</div>
                      <div className="mt-1 font-mono-tabular text-base font-semibold">
                        {unitCfg.formatDistance(selectedTrip.distanceMeters)} {unitCfg.distanceLabel}
                      </div>
                    </div>
                    <div className="rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] p-3 dark:border-[#2A3340] dark:bg-[#1E2630]">
                      <div className="text-[11px] uppercase text-[#5A6573] dark:text-[#9AA4B2]">Duration</div>
                      <div className="mt-1 font-mono-tabular text-base font-semibold">
                        {formatDuration(selectedTrip.durationMs)}
                      </div>
                    </div>
                    <div className="rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] p-3 dark:border-[#2A3340] dark:bg-[#1E2630]">
                      <div className="text-[11px] uppercase text-[#5A6573] dark:text-[#9AA4B2]">Max Speed</div>
                      <div className="mt-1 font-mono-tabular text-base font-semibold text-[#651FFF] dark:text-[#7C4DFF]">
                        {Math.round(unitCfg.toDisplay(selectedTrip.maxSpeedMps))} {unitCfg.label}
                      </div>
                    </div>
                    <div className="rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] p-3 dark:border-[#2A3340] dark:bg-[#1E2630]">
                      <div className="text-[11px] uppercase text-[#5A6573] dark:text-[#9AA4B2]">Avg Speed</div>
                      <div className="mt-1 font-mono-tabular text-base font-semibold text-[#0091EA] dark:text-[#00E5FF]">
                        {Math.round(unitCfg.toDisplay(selectedTrip.avgSpeedMps))} {unitCfg.label}
                      </div>
                    </div>
                  </div>

                  <div>
                    <h3 className="mb-2 text-xs font-medium tracking-wider uppercase text-[#5A6573] dark:text-[#9AA4B2]">
                      GPS Route Track
                    </h3>
                    <RoutePreview points={selectedTrip.points} />
                  </div>

                  <div>
                    <h3 className="mb-2 text-xs font-medium tracking-wider uppercase text-[#5A6573] dark:text-[#9AA4B2]">
                      Speed Over Time
                    </h3>
                    <SpeedChart
                      points={selectedTrip.points}
                      unit={unit}
                      startTimeMs={selectedTrip.startTimeMs}
                    />
                  </div>
                </div>
              ) : (
                <div className="flex h-64 items-center justify-center text-sm text-[#5A6573] dark:text-[#9AA4B2]">
                  Select a trip to inspect its route trace, speed graph, and GPX/CSV exports.
                </div>
              )}
            </div>
          </div>
        )}

        {activeSection === 'android' && <AndroidSpecPanel />}
      </main>
    </div>
  );
}

export default App;
