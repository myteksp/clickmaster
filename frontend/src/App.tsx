import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './components/Toast';
import ErrorBoundary from './components/ErrorBoundary';
import { setUnauthorizedHandler } from './api/client';
import Layout from './pages/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import CampaignsPage from './pages/CampaignsPage';
import CampaignDetailPage from './pages/CampaignDetailPage';
import CampaignFormPage from './pages/CampaignFormPage';
import ScenariosPage from './pages/ScenariosPage';
import ScenarioFormPage from './pages/ScenarioFormPage';
import SitesPage from './pages/SitesPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<DashboardPage />} />
        <Route path="campaigns" element={<CampaignsPage />} />
        <Route path="campaigns/new" element={<CampaignFormPage />} />
        <Route path="campaigns/:id/edit" element={<CampaignFormPage />} />
        <Route path="campaigns/:id" element={<CampaignDetailPage />} />
        <Route path="scenarios" element={<ScenariosPage />} />
        <Route path="scenarios/new" element={<ScenarioFormPage />} />
        <Route path="scenarios/:id" element={<ScenarioFormPage />} />
        <Route path="sites" element={<SitesPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function UnauthorizedHandler() {
  const { logout } = useAuth();
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
      window.location.href = '/login';
    });
    return () => setUnauthorizedHandler(null);
  }, [logout]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <ErrorBoundary>
        <AuthProvider>
          <ToastProvider>
            <UnauthorizedHandler />
            <AppRoutes />
          </ToastProvider>
        </AuthProvider>
      </ErrorBoundary>
    </BrowserRouter>
  );
}
