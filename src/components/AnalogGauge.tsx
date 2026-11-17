import React, { useEffect, useRef } from 'react';
import { SpeedUnit, ThemeMode, UNIT_CONFIG, bearingToCardinal } from '../types/velocity';

interface AnalogGaugeProps {
  speedMps: number;
  maxSpeedMps: number;
  unit: SpeedUnit;
  speedLimitEnabled: boolean;
  speedLimitMps: number;
  bearingDeg: number;
  theme: ThemeMode;
  onCycleUnit: () => void;
}

export const AnalogGauge: React.FC<AnalogGaugeProps> = ({
  speedMps,
  maxSpeedMps,
  unit,
  speedLimitEnabled,
  speedLimitMps,
  bearingDeg,
  theme,
  onCycleUnit,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const needleRef = useRef<{ value: number; velocity: number }>({ value: 0, velocity: 0 });
  const targetRef = useRef<number>(0);

  const config = UNIT_CONFIG[unit];
  const displaySpeed = config.toDisplay(speedMps);
  const displayMax = config.toDisplay(maxSpeedMps);
  const displayLimit = config.toDisplay(speedLimitMps);
  const isOverLimit = speedLimitEnabled && speedMps > speedLimitMps;
  const isWarning = !isOverLimit && speedLimitEnabled && speedMps >= speedLimitMps * 0.9;

  targetRef.current = Math.min(config.gaugeMax, Math.max(0, displaySpeed));

  useEffect(() => {
    let rafId = 0;
    let lastTime = performance.now();

    const draw = (now: number) => {
      const dt = Math.min(0.05, (now - lastTime) / 1000);
      lastTime = now;

      // Spring physics matching Android spec (damping ~0.7, medium-low stiffness)
      const target = targetRef.current;
      const stiffness = 62;
      const damping = 11.2;
      const force = (target - needleRef.current.value) * stiffness;
      needleRef.current.velocity += (force - needleRef.current.velocity * damping) * dt;
      needleRef.current.value += needleRef.current.velocity * dt;

      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          const dpr = window.devicePixelRatio || 1;
          const size = canvas.clientWidth;
          const targetPx = Math.round(size * dpr);
          if (canvas.width !== targetPx || canvas.height !== targetPx) {
            canvas.width = targetPx;
            canvas.height = targetPx;
          }

          ctx.save();
          ctx.scale(dpr, dpr);
          ctx.clearRect(0, 0, size, size);

          const cx = size / 2;
          const cy = size / 2;
          const radius = size * 0.405;
          const startAngle = (135 * Math.PI) / 180;
          const totalSweep = (270 * Math.PI) / 180;
          const endAngle = startAngle + totalSweep;

          const isDark = theme === 'dark';
          const trackColor = isDark ? '#2A3340' : '#DCE3EC';
          const tickColor = isDark ? '#475569' : '#94A3B8';
          const labelColor = isDark ? '#9AA4B2' : '#5A6573';
          const hubFill = isDark ? '#1E2630' : '#FFFFFF';
          const primaryAccent = isDark ? '#00E5FF' : '#0091EA';

          // Background 270 deg track
          ctx.beginPath();
          ctx.arc(cx, cy, radius, startAngle, endAngle);
          ctx.strokeStyle = trackColor;
          ctx.lineWidth = 14;
          ctx.lineCap = 'round';
          ctx.stroke();

          // Red speed-limit zone band from limit to scale max
          if (speedLimitEnabled && displayLimit < config.gaugeMax) {
            const limitFrac = Math.max(0, Math.min(1, displayLimit / config.gaugeMax));
            const limitAngle = startAngle + totalSweep * limitFrac;
            ctx.beginPath();
            ctx.arc(cx, cy, radius, limitAngle, endAngle);
            ctx.strokeStyle = 'rgba(255, 23, 68, 0.32)';
            ctx.lineWidth = 14;
            ctx.lineCap = 'butt';
            ctx.stroke();
          }

          // Progress gradient arc
          const animatedFrac = Math.max(0, Math.min(1, needleRef.current.value / config.gaugeMax));
          if (animatedFrac > 0.002) {
            const progressEnd = startAngle + totalSweep * animatedFrac;
            const grad = ctx.createLinearGradient(cx - radius, cy + radius, cx + radius, cy - radius);
            if (isOverLimit) {
              grad.addColorStop(0, '#FF1744');
              grad.addColorStop(1, '#FF5252');
            } else {
              grad.addColorStop(0, '#00E676');
              grad.addColorStop(0.45, primaryAccent);
              grad.addColorStop(0.78, '#FFB300');
              grad.addColorStop(1, '#FF1744');
            }

            // Soft outer glow (40% alpha)
            ctx.save();
            ctx.beginPath();
            ctx.arc(cx, cy, radius, startAngle, progressEnd);
            ctx.strokeStyle = isOverLimit ? 'rgba(255, 23, 68, 0.38)' : 'rgba(0, 229, 255, 0.35)';
            ctx.lineWidth = 22;
            ctx.lineCap = 'round';
            ctx.filter = 'blur(6px)';
            ctx.stroke();
            ctx.restore();

            ctx.beginPath();
            ctx.arc(cx, cy, radius, startAngle, progressEnd);
            ctx.strokeStyle = grad;
            ctx.lineWidth = 14;
            ctx.lineCap = 'round';
            ctx.stroke();
          }

          // Major (every 20) & minor (every 10) ticks
          const step = 10;
          for (let val = 0; val <= config.gaugeMax; val += step) {
            const frac = val / config.gaugeMax;
            const angle = startAngle + totalSweep * frac;
            const isMajor = val % 20 === 0;
            const outerR = radius - 14;
            const innerR = outerR - (isMajor ? 12 : 6);

            ctx.beginPath();
            ctx.moveTo(cx + Math.cos(angle) * innerR, cy + Math.sin(angle) * innerR);
            ctx.lineTo(cx + Math.cos(angle) * outerR, cy + Math.sin(angle) * outerR);
            ctx.strokeStyle =
              speedLimitEnabled && val >= displayLimit ? '#FF1744' : isMajor ? labelColor : tickColor;
            ctx.lineWidth = isMajor ? 2 : 1.25;
            ctx.stroke();

            if (isMajor) {
              const textR = innerR - 15;
              const tx = cx + Math.cos(angle) * textR;
              const ty = cy + Math.sin(angle) * textR;
              ctx.fillStyle = labelColor;
              ctx.font = '600 11px "JetBrains Mono", monospace';
              ctx.textAlign = 'center';
              ctx.textBaseline = 'middle';
              ctx.fillText(String(val), tx, ty);
            }
          }

          // Violet triangle marker at trip max speed
          if (displayMax > 1) {
            const maxFrac = Math.min(1, displayMax / config.gaugeMax);
            const maxAngle = startAngle + totalSweep * maxFrac;
            const tipR = radius + 12;
            const baseR = radius + 21;
            const spread = 0.04;
            ctx.beginPath();
            ctx.moveTo(cx + Math.cos(maxAngle) * tipR, cy + Math.sin(maxAngle) * tipR);
            ctx.lineTo(
              cx + Math.cos(maxAngle - spread) * baseR,
              cy + Math.sin(maxAngle - spread) * baseR
            );
            ctx.lineTo(
              cx + Math.cos(maxAngle + spread) * baseR,
              cy + Math.sin(maxAngle + spread) * baseR
            );
            ctx.closePath();
            ctx.fillStyle = '#7C4DFF';
            ctx.fill();
          }

          // Tapered needle
          const needleAngle = startAngle + totalSweep * animatedFrac;
          const needleTipR = radius - 18;
          const perpAngle = needleAngle + Math.PI / 2;
          ctx.beginPath();
          ctx.moveTo(cx + Math.cos(needleAngle) * needleTipR, cy + Math.sin(needleAngle) * needleTipR);
          ctx.lineTo(cx + Math.cos(perpAngle) * 4.5, cy + Math.sin(perpAngle) * 4.5);
          ctx.lineTo(cx - Math.cos(needleAngle) * 14, cy - Math.sin(needleAngle) * 14);
          ctx.lineTo(cx - Math.cos(perpAngle) * 4.5, cy - Math.sin(perpAngle) * 4.5);
          ctx.closePath();
          ctx.fillStyle = isOverLimit ? '#FF1744' : primaryAccent;
          ctx.fill();

          // Center hub circle with 2px cyan ring
          ctx.beginPath();
          ctx.arc(cx, cy, 9, 0, Math.PI * 2);
          ctx.fillStyle = hubFill;
          ctx.fill();
          ctx.lineWidth = 2;
          ctx.strokeStyle = isOverLimit ? '#FF1744' : primaryAccent;
          ctx.stroke();

          ctx.restore();
        }
      }

      rafId = requestAnimationFrame(draw);
    };

    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [config.gaugeMax, displayLimit, displayMax, isOverLimit, speedLimitEnabled, theme]);

  const roundedSpeed = Math.round(displaySpeed);
  const speedTextColor = isOverLimit
    ? 'text-[#FF1744] motion-safe:animate-pulse'
    : isWarning
      ? 'text-[#FFB300]'
      : 'text-[#0B0F14] dark:text-white';

  return (
    <div className="relative mx-auto flex aspect-square w-full max-w-[360px] items-center justify-center">
      <canvas
        ref={canvasRef}
        className="h-full w-full"
        aria-label={`Speedometer gauge reading ${roundedSpeed} ${config.label}`}
      />
      <div className="pointer-events-none absolute inset-x-0 bottom-7 flex flex-col items-center">
        <span className={`font-speed text-5xl font-bold tracking-tight ${speedTextColor}`}>
          {roundedSpeed}
        </span>
        <button
          type="button"
          onClick={onCycleUnit}
          title="Tap to switch speed unit"
          className="pointer-events-auto mt-1 min-h-[40px] rounded-md px-3 py-1 text-xs font-medium tracking-[0.08em] uppercase text-[#5A6573] transition-colors hover:text-[#0091EA] focus-visible:outline-2 focus-visible:outline-[#00E5FF] dark:text-[#9AA4B2] dark:hover:text-[#00E5FF]"
        >
          {config.label} · {bearingToCardinal(bearingDeg)} ({Math.round(bearingDeg)}°)
        </button>
      </div>
    </div>
  );
};
