import React, { useState } from 'react';
import { FlipHorizontal, X } from 'lucide-react';
import { SpeedUnit, UNIT_CONFIG, bearingToCardinal } from '../types/velocity';

interface HudOverlayProps {
  speedMps: number;
  unit: SpeedUnit;
  bearingDeg: number;
  speedLimitEnabled: boolean;
  speedLimitMps: number;
  onCycleUnit: () => void;
  onExit: () => void;
}

export const HudOverlay: React.FC<HudOverlayProps> = ({
  speedMps,
  unit,
  bearingDeg,
  speedLimitEnabled,
  speedLimitMps,
  onCycleUnit,
  onExit,
}) => {
  const [mirrored, setMirrored] = useState(true);
  const cfg = UNIT_CONFIG[unit];
  const displaySpeed = Math.round(cfg.toDisplay(speedMps));
  const isOverLimit = speedLimitEnabled && speedMps > speedLimitMps;

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-between bg-black p-6 text-white select-none">
      <div className="flex items-center justify-between gap-4">
        <button
          type="button"
          onClick={() => setMirrored((prev) => !prev)}
          className="inline-flex min-h-[44px] items-center gap-2 rounded-full border border-[#2A3340] bg-[#141A22] px-4 py-2 text-xs font-medium whitespace-nowrap text-[#9AA4B2] transition-colors hover:border-[#00E5FF] hover:text-white focus-visible:outline-2 focus-visible:outline-[#00E5FF]"
        >
          <FlipHorizontal className="h-4 w-4" aria-hidden="true" />
          {mirrored ? 'Windshield Mirror: On' : 'Windshield Mirror: Off'}
        </button>

        <button
          type="button"
          onClick={onExit}
          aria-label="Exit HUD mode"
          className="inline-flex min-h-[44px] items-center gap-2 rounded-full border border-[#2A3340] bg-[#141A22] px-4 py-2 text-xs font-medium whitespace-nowrap text-white transition-colors hover:border-[#FF1744] hover:text-[#FF1744] focus-visible:outline-2 focus-visible:outline-[#00E5FF]"
        >
          <X className="h-4 w-4" aria-hidden="true" />
          Exit HUD
        </button>
      </div>

      <div
        className="flex flex-col items-center justify-center transition-transform duration-200"
        style={{ transform: mirrored ? 'scaleX(-1)' : 'none' }}
      >
        <div
          className={`font-speed text-[clamp(5.5rem,18vw,10.5rem)] leading-none font-extrabold tracking-tight ${
            isOverLimit ? 'text-[#FF1744] motion-safe:animate-pulse' : 'text-[#00E5FF]'
          }`}
        >
          {displaySpeed}
        </div>
        <button
          type="button"
          onClick={onCycleUnit}
          className="mt-4 min-h-[44px] px-4 text-xl font-semibold tracking-[0.14em] uppercase text-[#9AA4B2] transition-colors hover:text-white"
        >
          {cfg.label} · {bearingToCardinal(bearingDeg)}
        </button>
        {isOverLimit && (
          <p className="mt-2 text-sm font-semibold tracking-wider uppercase text-[#FF1744]">
            Speed Limit Exceeded ({Math.round(cfg.toDisplay(speedLimitMps))} {cfg.label})
          </p>
        )}
      </div>

      <p className="text-center text-xs text-[#9AA4B2]">
        Pure black #000000 windshield projection canvas. Tap unit label to cycle km/h, mph, knots.
      </p>
    </div>
  );
};
