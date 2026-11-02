import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import SettingsPage from './pages/Settings';

import Clients from './pages/Clients';
import ClientNew from './pages/ClientNew';
import ClientDetail from './pages/ClientDetail';
import Invoices from './pages/Invoices';
import InvoiceNew from './pages/InvoiceNew';
import InvoiceEdit from './pages/InvoiceEdit';
import InvoiceDetail from './pages/InvoiceDetail';
import Proposals from './pages/Proposals';
import ProposalNew from './pages/ProposalNew';
import ProposalEdit from './pages/ProposalEdit';
import ProposalDetail from './pages/ProposalDetail';
import PublicProposalPage from './pages/PublicProposal';
import PublicInvoicePage from './pages/PublicInvoice';
import PaymentSuccessPage from './pages/PaymentSuccess';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/p/:token" element={<PublicProposalPage />} />
          <Route path="/inv/:token" element={<PublicInvoicePage />} />
          <Route path="/inv/:token/success" element={<PaymentSuccessPage />} />

          {/* Protected Workspaces */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/clients" element={<Clients />} />
            <Route path="/clients/new" element={<ClientNew />} />
            <Route path="/clients/:id" element={<ClientDetail />} />
            <Route path="/invoices" element={<Invoices />} />
            <Route path="/invoices/new" element={<InvoiceNew />} />
            <Route path="/invoices/:id" element={<InvoiceDetail />} />
            <Route path="/invoices/:id/edit" element={<InvoiceEdit />} />
            <Route path="/proposals" element={<Proposals />} />
            <Route path="/proposals/new" element={<ProposalNew />} />
            <Route path="/proposals/:id" element={<ProposalDetail />} />
            <Route path="/proposals/:id/edit" element={<ProposalEdit />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>

          {/* Catch all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
