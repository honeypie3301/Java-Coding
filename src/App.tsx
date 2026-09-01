import React from 'react';
import { Terminal, Shield } from 'lucide-react';

export default function App() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6 font-sans">
      <div className="max-w-md w-full bg-slate-900/60 border border-slate-800 rounded-2xl p-6 text-center space-y-4 shadow-xl">
        <div className="mx-auto w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-slate-100">Backwoods Mod Workspace</h1>
          <p className="text-xs text-slate-400 mt-1">
            Active workspace for Rot &amp; StiltWalker AI procedures (NeoForge 1.21.1 &amp; 1.21.8).
          </p>
        </div>
        <div className="flex items-center justify-center gap-2 pt-2">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-slate-800 text-slate-300 text-xs font-mono rounded-full border border-slate-700">
            <Terminal className="w-3.5 h-3.5 text-emerald-400" /> Mod AI Engine Active
          </span>
        </div>
      </div>
    </div>
  );
}
