import React from 'react';
import { Copy, Check } from 'lucide-react';

const MODULES = [
  {
    layer: 'Domain Layer',
    path: 'app/src/main/java/com/velocity/speedometer/domain/',
    summary:
      'Pure Kotlin models (Trip, TrackPoint, LiveTelemetry, UserSettings), SpeedConverter (internal m/s to km/h, mph, knots), 1D Kalman SpeedSmoother, GeoUtils distance/heading math, and TripExporter (GPX 1.1 & CSV).',
  },
  {
    layer: 'Data & Location Layer',
    path: 'app/src/main/java/com/velocity/speedometer/data/',
    summary:
      'Room database with foreign-key cascade delete (TripDao, VelocityDatabase), Preferences DataStore for settings, lifetime odometer, and process-death recovery snapshots, plus FusedLocationSource (HIGH_ACCURACY, 500 ms interval).',
  },
  {
    layer: 'Foreground Service & Alerts',
    path: 'app/src/main/java/com/velocity/speedometer/service/',
    summary:
      'TrackingForegroundService (Android 14+ foregroundServiceType="location") with live speed/distance notification and Pause/Resume/Stop actions, TrackingManager singleton StateFlow controller, and SpeedAlertManager (vibration + tone).',
  },
  {
    layer: 'Jetpack Compose UI & Maps',
    path: 'app/src/main/java/com/velocity/speedometer/ui/',
    summary:
      'Material 3 single-activity navigation across Splash, Permission, Analog/Digital Speedometer, Windshield HUD, Trip History, Trip Detail (Google Maps Compose with automatic osmdroid OpenStreetMap fallback), Settings, and About.',
  },
];

const COMMANDS = [
  { label: 'Run JVM Unit Tests', cmd: 'gradle testDebugUnitTest' },
  { label: 'Run Android Lint', cmd: 'gradle lintDebug' },
  { label: 'Build Debug APK', cmd: 'gradle assembleDebug' },
  { label: 'Run Compose UI Test', cmd: 'gradle connectedDebugAndroidTest' },
];

export const AndroidSpecPanel: React.FC = () => {
  const [copiedCmd, setCopiedCmd] = React.useState<string | null>(null);

  const handleCopy = async (cmd: string) => {
    try {
      await navigator.clipboard.writeText(cmd);
      setCopiedCmd(cmd);
      window.setTimeout(() => setCopiedCmd(null), 1500);
    } catch {
      setCopiedCmd(null);
    }
  };

  return (
    <div className="space-y-8">
      <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-6 dark:border-[#2A3340] dark:bg-[#141A22]">
        <h2 className="text-xl font-semibold text-[#0B0F14] dark:text-white">
          Native Android Project Architecture
        </h2>
        <p className="mt-2 max-w-3xl text-sm leading-relaxed text-[#5A6573] dark:text-[#9AA4B2]">
          The complete Kotlin & Jetpack Compose Android application resides in the repository root
          under <code className="font-mono-tabular text-[#0091EA] dark:text-[#00E5FF]">app/</code>{' '}
          with Gradle Kotlin DSL, Hilt dependency injection, Room persistence, and GitHub Actions CI.
        </p>

        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
          {MODULES.map((mod) => (
            <div
              key={mod.layer}
              className="rounded-2xl border border-[#DCE3EC] bg-[#F5F7FA] p-5 dark:border-[#2A3340] dark:bg-[#1E2630]"
            >
              <h3 className="text-base font-semibold text-[#0B0F14] dark:text-white">
                {mod.layer}
              </h3>
              <p className="mt-1 font-mono-tabular text-xs text-[#0091EA] dark:text-[#00E5FF]">
                {mod.path}
              </p>
              <p className="mt-2.5 text-sm leading-relaxed text-[#5A6573] dark:text-[#9AA4B2]">
                {mod.summary}
              </p>
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-6 dark:border-[#2A3340] dark:bg-[#141A22]">
          <h3 className="text-lg font-semibold text-[#0B0F14] dark:text-white">
            Gradle Build & Verification Commands
          </h3>
          <p className="mt-1 text-sm text-[#5A6573] dark:text-[#9AA4B2]">
            Min SDK 26 (Android 8.0) · Target & Compile SDK 35 · CI workflow in{' '}
            <code className="font-mono-tabular">.github/workflows/android-ci.yml</code>
          </p>
          <div className="mt-4 space-y-3">
            {COMMANDS.map((item) => (
              <div
                key={item.cmd}
                className="flex items-center justify-between gap-3 rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] px-4 py-3 dark:border-[#2A3340] dark:bg-[#0B0F14]"
              >
                <div className="min-w-0">
                  <div className="text-xs font-medium text-[#5A6573] dark:text-[#9AA4B2]">
                    {item.label}
                  </div>
                  <code className="font-mono-tabular text-sm text-[#0B0F14] dark:text-white">
                    {item.cmd}
                  </code>
                </div>
                <button
                  type="button"
                  onClick={() => handleCopy(item.cmd)}
                  aria-label={`Copy ${item.cmd}`}
                  className="inline-flex min-h-[40px] min-w-[40px] shrink-0 items-center justify-center rounded-lg border border-[#DCE3EC] bg-white text-[#5A6573] transition-colors hover:border-[#0091EA] hover:text-[#0091EA] dark:border-[#2A3340] dark:bg-[#1E2630] dark:text-[#9AA4B2] dark:hover:border-[#00E5FF] dark:hover:text-[#00E5FF]"
                >
                  {copiedCmd === item.cmd ? (
                    <Check className="h-4 w-4 text-[#00E676]" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-[20px] border border-[#DCE3EC] bg-white p-6 dark:border-[#2A3340] dark:bg-[#141A22]">
          <h3 className="text-lg font-semibold text-[#0B0F14] dark:text-white">
            Google Maps & Offline Fallback
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-[#5A6573] dark:text-[#9AA4B2]">
            Trip detail screens automatically render Google Maps Compose when an API key is supplied
            in <code className="font-mono-tabular">local.properties</code>, and switch seamlessly to
            OpenStreetMap (<code className="font-mono-tabular">osmdroid</code>) when offline or when
            no key is configured.
          </p>
          <pre className="mt-4 overflow-x-auto rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] p-4 font-mono-tabular text-xs text-[#0B0F14] dark:border-[#2A3340] dark:bg-[#0B0F14] dark:text-[#00E5FF]">
            {`# local.properties (git-ignored)
MAPS_API_KEY=YOUR_GOOGLE_MAPS_ANDROID_API_KEY`}
          </pre>
          <p className="mt-4 text-xs text-[#5A6573] dark:text-[#9AA4B2]">
            Core telemetry, Kalman filtering, Room trip storage, GPX/CSV export, and HUD mode
            operate 100% offline with zero network dependency.
          </p>
        </div>
      </div>
    </div>
  );
};
