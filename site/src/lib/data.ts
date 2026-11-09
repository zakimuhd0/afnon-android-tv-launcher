// The app's data, read from its Java sources by scripts/extract.mjs.
import citiesJson from '../data/cities.json';
import statesJson from '../data/states.json';
import stationsJson from '../data/stations.json';

export type City = (typeof citiesJson)[number];
export type Station = (typeof stationsJson)[number];

export const CITIES: City[] = citiesJson;
export const STATES: string[] = statesJson;
export const STATIONS: Station[] = stationsJson;

/** Viewer preferences, as Afnon Settings keeps them on the TV. Saved in this browser only. */
export type Prefs = {
  city: string;
  method: string | null;
  hanafi: boolean;
  hijriOffset: number;
  clock: 'system' | '12' | '24';
  digits: 'auto' | 'eastern' | 'western';
  remind: number;
  adhan: boolean;
  station: string;
  voice?: string; voiceFajr?: string; // adhan voices (Adhans ids); unset: the app's defaults
  radioResume?: boolean; radioDark?: number; // Settings' radio rows
  adjust?: number[]; // Settings → Adjust the times: minutes added to Fajr…Isha, −30 to +30 (Prefs.adjustments)
  night?: boolean; // night mode (Prefs.night): tokens.night.json's palette
};

/** The six adjustments, zeros when unset. */
export const adjustOf = (p: Prefs) => [0, 1, 2, 3, 4, 5].map((i) => Math.max(-30, Math.min(30, p.adjust?.[i] ?? 0)));

export const DEFAULT_PREFS: Prefs = {
  city: 'WLY01', method: null, hanafi: false, hijriOffset: 0, clock: 'system', digits: 'auto',
  remind: 15, adhan: true, station: 'ikim',
};

export const cityOf = (p: Prefs) => CITIES.find((c) => c.id === p.city) ?? CITIES[0];
export const stationOf = (p: Prefs) => STATIONS.find((s) => s.id === p.station) ?? STATIONS[0];
