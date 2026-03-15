import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/common/ProtectedRoute';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/public/LoginPage';
import RegisterPage from './pages/public/RegisterPage';
import TenderListPage from './pages/public/TenderListPage';

// Lazy load portal pages
import { lazy, Suspense } from 'react';
import { Spin } from 'antd';

const TendereeDashboard = lazy(() => import('./pages/tenderee/DashboardPage'));
const CreateTender = lazy(() => import('./pages/tenderee/CreateTenderPage'));
const TenderDetail = lazy(() => import('./pages/tenderee/TenderDetailPage'));
const TendererDashboard = lazy(() => import('./pages/tenderer/DashboardPage'));
const SubmitBid = lazy(() => import('./pages/tenderer/SubmitBidPage'));
const Qualification = lazy(() => import('./pages/tenderer/QualificationPage'));
const EvaluatorDashboard = lazy(() => import('./pages/evaluator/DashboardPage'));
const EvaluationPage = lazy(() => import('./pages/evaluator/EvaluationPage'));
const AuditLogPage = lazy(() => import('./pages/admin/AuditLogPage'));
const ReportsPage = lazy(() => import('./pages/admin/ReportsPage'));

const queryClient = new QueryClient();

const Loading = () => <div style={{ display: 'flex', justifyContent: 'center', padding: 100 }}><Spin size="large" /></div>;

const theme = {
  token: {
    colorPrimary: '#1a365d',
    borderRadius: 6,
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
};

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider theme={theme}>
        <BrowserRouter>
          <Suspense fallback={<Loading />}>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/tenders/browse" element={<TenderListPage />} />

              {/* Protected routes inside layout */}
              <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
                <Route index element={<Navigate to="/dashboard" replace />} />

                {/* Dashboard - role-based */}
                <Route path="dashboard" element={
                  <Suspense fallback={<Loading />}>
                    <RoleDashboard />
                  </Suspense>
                } />

                {/* Tenderee routes */}
                <Route path="tenders" element={<ProtectedRoute roles={['TENDEREE']}><TendereeDashboard /></ProtectedRoute>} />
                <Route path="tenders/create" element={<ProtectedRoute roles={['TENDEREE']}><CreateTender /></ProtectedRoute>} />
                <Route path="tenders/:tenderId" element={<TenderDetail />} />

                {/* Tenderer routes */}
                <Route path="bids" element={<ProtectedRoute roles={['TENDERER']}><TendererDashboard /></ProtectedRoute>} />
                <Route path="bids/submit/:tenderId" element={<ProtectedRoute roles={['TENDERER']}><SubmitBid /></ProtectedRoute>} />
                <Route path="qualification" element={<ProtectedRoute roles={['TENDERER']}><Qualification /></ProtectedRoute>} />

                {/* Evaluator routes */}
                <Route path="evaluations" element={<ProtectedRoute roles={['EVALUATOR', 'COMMITTEE']}><EvaluatorDashboard /></ProtectedRoute>} />
                <Route path="evaluations/:tenderId/:bidId" element={<ProtectedRoute roles={['EVALUATOR', 'COMMITTEE']}><EvaluationPage /></ProtectedRoute>} />

                {/* Admin routes */}
                <Route path="admin/audit" element={<ProtectedRoute roles={['TENDEREE']}><AuditLogPage /></ProtectedRoute>} />
                <Route path="admin/reports" element={<ProtectedRoute roles={['TENDEREE']}><ReportsPage /></ProtectedRoute>} />
                <Route path="reports" element={<ProtectedRoute roles={['TENDEREE']}><ReportsPage /></ProtectedRoute>} />

                {/* Shared routes */}
                <Route path="contracts" element={<div>Contracts page - coming soon</div>} />
              </Route>

              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </ConfigProvider>
    </QueryClientProvider>
  );
}

// Route to correct dashboard based on role
function RoleDashboard() {
  const role = useAuthStore((s) => s.role);
  switch (role) {
    case 'TENDEREE': return <TendereeDashboard />;
    case 'TENDERER': return <TendererDashboard />;
    case 'EVALUATOR':
    case 'COMMITTEE': return <EvaluatorDashboard />;
    default: return <TendereeDashboard />;
  }
}
