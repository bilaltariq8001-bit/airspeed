import React, { useState } from 'react';
import { SpeedUnit, TrackSample, UNIT_CONFIG, formatDuration } from '../types/velocity';

interface SpeedChartProps {
  points: TrackSample[];
  unit: SpeedUnit;
  startTimeMs: number;
}

export const SpeedChart: React.FC<SpeedChartProps> = ({ points, unit, startTimeMs }) => {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const unitCfg = UNIT_CONFIG[unit];

  if (points.length < 2) {
    return (
      <div className="flex h-48 items-center justify-center rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] text-sm text-[#5A6573] dark:border-[#2A3340] dark:bg-[#0B0F14] dark:text-[#9AA4B2]">
        Not enough GPS points yet to plot speed profile.
      </div>
    );
  }

  const width = 640;
  const height = 210;
  const padL = 42;
  const padR = 16;
  const padT = 16;
  const padB = 28;
  const plotW = width - padL - padR;
  const plotH = height - padT - padB;

  const speeds = points.map((p) => unitCfg.toDisplay(p.speedMps));
  const maxVal = Math.max(20, Math.ceil(Math.max(...speeds) / 20) * 20);

  const coords = points.map((p, i) => {
    const x = padL + (i / (points.length - 1)) * plotW;
    const y = padT + plotH - (unitCfg.toDisplay(p.speedMps) / maxVal) * plotH;
    return { x, y, speed: unitCfg.toDisplay(p.speedMps), elapsedMs: p.timestampMs - startTimeMs };
  });

  const linePath = coords.map((c, idx) => `${idx === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
  const areaPath = `${linePath} L ${coords[coords.length - 1].x.toFixed(1)} ${(padT + plotH).toFixed(1)} L ${coords[0].x.toFixed(1)} ${(padT + plotH).toFixed(1)} Z`;

  const active = hoverIndex !== null ? coords[hoverIndex] : null;

  return (
    <div className="relative w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="h-52 w-full cursor-crosshair select-none"
        onMouseLeave={() => setHoverIndex(null)}
        onMouseMove={(e) => {
          const rect = e.currentTarget.getBoundingClientRect();
          const relX = ((e.clientX - rect.left) / rect.width) * width;
          const clamped = Math.max(padL, Math.min(width - padR, relX));
          const ratio = (clamped - padL) / plotW;
          setHoverIndex(Math.min(coords.length - 1, Math.max(0, Math.round(ratio * (coords.length - 1)))));
        }}
      >
        <defs>
          <linearGradient id="speedFillGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#00E5FF" stopOpacity="0.28" />
            <stop offset="100%" stopColor="#00E5FF" stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {[0, 0.5, 1].map((frac) => {
          const y = padT + plotH * (1 - frac);
          const val = Math.round(maxVal * frac);
          return (
            <g key={frac}>
              <line
                x1={padL}
                y1={y}
                x2={width - padR}
                y2={y}
                className="stroke-[#DCE3EC] dark:stroke-[#2A3340]"
                strokeDasharray="3 3"
              />
              <text
                x={padL - 8}
                y={y + 3}
                textAnchor="end"
                className="fill-[#5A6573] text-[10px] dark:fill-[#9AA4B2]"
                style={{ fontFamily: 'JetBrains Mono, monospace' }}
              >
                {val}
              </text>
            </g>
          );
        })}

        <path d={areaPath} fill="url(#speedFillGrad)" />
        <path d={linePath} fill="none" stroke="#00E5FF" strokeWidth="2.5" strokeLinejoin="round" />

        {active && (
          <g>
            <line
              x1={active.x}
              y1={padT}
              x2={active.x}
              y2={padT + plotH}
              stroke="#7C4DFF"
              strokeWidth="1.25"
              strokeDasharray="3 3"
            />
            <circle cx={active.x} cy={active.y} r="4.5" fill="#00E5FF" stroke="#0B0F14" strokeWidth="2" />
          </g>
        )}

        <text
          x={padL}
          y={height - 6}
          className="fill-[#5A6573] text-[10px] dark:fill-[#9AA4B2]"
          style={{ fontFamily: 'JetBrains Mono, monospace' }}
        >
          00:00
        </text>
        <text
          x={width - padR}
          y={height - 6}
          textAnchor="end"
          className="fill-[#5A6573] text-[10px] dark:fill-[#9AA4B2]"
          style={{ fontFamily: 'JetBrains Mono, monospace' }}
        >
          {formatDuration(points[points.length - 1].timestampMs - startTimeMs)}
        </text>
      </svg>

      {active && (
        <div className="mt-1 flex items-center justify-between text-xs font-mono-tabular text-[#5A6573] dark:text-[#9AA4B2]">
          <span>Sample Elapsed: {formatDuration(active.elapsedMs)}</span>
          <span className="font-semibold text-[#0091EA] dark:text-[#00E5FF]">
            {active.speed.toFixed(1)} {unitCfg.label}
          </span>
        </div>
      )}
    </div>
  );
};

interface RoutePreviewProps {
  points: TrackSample[];
}

export const RoutePreview: React.FC<RoutePreviewProps> = ({ points }) => {
  if (points.length < 2) {
    return (
      <div className="flex h-48 items-center justify-center rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] text-sm text-[#5A6573] dark:border-[#2A3340] dark:bg-[#0B0F14] dark:text-[#9AA4B2]">
        Route path requires at least 2 coordinates.
      </div>
    );
  }

  const lats = points.map((p) => p.lat);
  const lngs = points.map((p) => p.lng);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  const spanLat = Math.max(0.0005, maxLat - minLat);
  const spanLng = Math.max(0.0005, maxLng - minLng);

  const w = 420;
  const h = 210;
  const pad = 24;

  const projected = points.map((p) => ({
    x: pad + ((p.lng - minLng) / spanLng) * (w - pad * 2),
    y: h - pad - ((p.lat - minLat) / spanLat) * (h - pad * 2),
  }));

  const path = projected.map((pt, idx) => `${idx === 0 ? 'M' : 'L'} ${pt.x.toFixed(1)} ${pt.y.toFixed(1)}`).join(' ');
  const start = projected[0];
  const end = projected[projected.length - 1];

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="h-52 w-full rounded-xl border border-[#DCE3EC] bg-[#F5F7FA] dark:border-[#2A3340] dark:bg-[#0B0F14]">
      <defs>
        <pattern id="routeGrid" width="28" height="28" patternUnits="userSpaceOnUse">
          <path d="M 28 0 L 0 0 0 28" fill="none" className="stroke-[#E2E8F0] dark:stroke-[#1E2630]" strokeWidth="1" />
        </pattern>
      </defs>
      <rect width={w} height={h} fill="url(#routeGrid)" />
      <path d={path} fill="none" stroke="#00E5FF" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={start.x} cy={start.y} r="5" fill="#00E676" stroke="#0B0F14" strokeWidth="1.5" />
      <circle cx={end.x} cy={end.y} r="5" fill="#FF1744" stroke="#0B0F14" strokeWidth="1.5" />
    </svg>
  );
};
