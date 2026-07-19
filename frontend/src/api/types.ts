export interface User {
  userId: string;
  email: string;
  name: string;
  token: string;
}

export interface Site {
  id: string;
  userId: string;
  name: string;
  baseUrl: string;
}

export interface NavigationStep {
  selector: string;
  text: string;
  waitAfterMs: number;
}

export interface ClickTarget {
  selector: string;
  text: string;
  tag: string;
  pagePath: string;
  navigationSteps: NavigationStep[];
  probability: number;
  delayBeforeMs: number;
  delayAfterMs: number;
}

export interface DiscoveredElement {
  selector: string;
  text: string;
  tag: string;
  href: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface PageState {
  id: string;
  label: string;
  screenshot: string;
  elements: DiscoveredElement[];
  pageWidth: number;
  pageHeight: number;
}

export interface SitePreview {
  states: PageState[];
}

export interface SessionState {
  sessionId: string;
  screenshot: string;
  elements: DiscoveredElement[];
  currentUrl: string;
  currentPath: NavigationStep[];
  pageWidth: number;
  pageHeight: number;
}

export interface Campaign {
  id: string;
  userId: string;
  siteId: string;
  siteName: string;
  siteBaseUrl: string;
  name: string;
  status: 'DRAFT' | 'READY' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  simulationLevel: 'HTTP_ONLY' | 'BROWSER_NAVIGATION' | 'FULL_BROWSER';
  trafficPattern: 'CONSTANT' | 'RAMP_UP' | 'PULSE' | 'REALISTIC_WAVE';
  visitsPerHour: number;
  durationMinutes: number;
  scheduleCron: string | null;
  geoDistribution: GeoDistribution[];
  deviceProfile: DeviceProfile[];
  userAgentConfig: UserAgentConfig;
  proxyConfig: ProxyConfig;
  clickTargets: ClickTarget[];
  scenarios: CampaignScenarioDto[];
  createdAt: string;
  updatedAt: string;
  lastRunAt: string | null;
}

export interface GeoDistribution {
  countryCode: string;
  countryName: string;
  city: string | null;
  weight: number;
}

export interface DeviceProfile {
  device: string;
  os: string;
  browser: string;
  weight: number;
}

export interface UserAgentConfig {
  rotation: string;
  customPool: string[];
}

export interface ProxyConfig {
  provider: string;
}

export interface CampaignScenarioDto {
  scenarioId: string;
  scenarioName: string;
  entryUrl: string | null;
  weight: number;
}

export interface CampaignRun {
  id: string;
  campaignId: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  startedAt: string;
  finishedAt: string | null;
  totalVisits: number;
  successfulVisits: number;
  failedVisits: number;
  stats: string | null;
}

export interface Scenario {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  steps: ScenarioStep[];
}

export interface ScenarioStep {
  id: string | null;
  orderIndex: number;
  actionType: 'LOAD' | 'CLICK' | 'SCROLL' | 'WAIT' | 'HOVER' | 'TYPE' | 'EXTRACT_TEXT' | 'SCREENSHOT' | 'CUSTOM_JS';
  selector: string | null;
  value: string | null;
  delayBeforeMs: number;
  delayAfterMs: number;
  probability: number;
  config: string | null;
}

export interface SimulationStats {
  campaignId: string;
  campaignRunId: string;
  totalVisits: number;
  successfulVisits: number;
  failedVisits: number;
  activeProxies: number;
  visitsPerSecond: number;
  elapsedTime: string;
  estimatedRemaining: string;
}

export interface VisitEvent {
  type: 'visit';
  runId: string;
  campaignId: string;
  path: string;
  statusCode: number;
  responseTimeMs: number;
  success: boolean;
  proxyAddress: string;
}
