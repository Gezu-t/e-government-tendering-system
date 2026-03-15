import apiClient from './client';
import type {
  AuthResponse, LoginRequest, RegistrationRequest,
  Tender, Bid, Evaluation, Contract, Page,
  VendorQualification, TenderAmendment, TenderStatus, TenderType,
} from '../types';

// ============================================================
// Auth API
// ============================================================
export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/api/auth/login', data),
  register: (data: RegistrationRequest) =>
    apiClient.post<AuthResponse>('/api/auth/register', data),
};

// ============================================================
// Tender API
// ============================================================
export const tenderApi = {
  getAll: (params?: { title?: string; status?: TenderStatus; type?: TenderType; page?: number; size?: number }) =>
    apiClient.get<Page<Tender>>('/api/tenders', { params }),
  getById: (id: number) =>
    apiClient.get<Tender>(`/api/tenders/${id}`),
  create: (data: Partial<Tender>) =>
    apiClient.post<Tender>('/api/tenders', data),
  publish: (id: number) =>
    apiClient.post<Tender>(`/api/tenders/${id}/publish`),
  close: (id: number) =>
    apiClient.post<Tender>(`/api/tenders/${id}/close`),
  amend: (id: number, data: { reason: string; description?: string; newSubmissionDeadline?: string }) =>
    apiClient.post<Tender>(`/api/tenders/${id}/amend`, data),
  getAmendments: (id: number) =>
    apiClient.get<TenderAmendment[]>(`/api/tenders/${id}/amendments`),
  getMyTenders: (params?: { page?: number; size?: number }) =>
    apiClient.get<Page<Tender>>('/api/tenders/tenderee', { params }),
  // Pre-bid clarifications
  getClarifications: (tenderId: number) =>
    apiClient.get(`/api/tenders/${tenderId}/clarifications/public`),
  askQuestion: (tenderId: number, data: { question: string; category?: string; organizationName?: string }) =>
    apiClient.post(`/api/tenders/${tenderId}/clarifications`, data),
  answerQuestion: (tenderId: number, clarificationId: number, data: { answer: string; makePublic?: boolean }) =>
    apiClient.put(`/api/tenders/${tenderId}/clarifications/${clarificationId}/answer`, data),
};

// ============================================================
// Bid API
// ============================================================
export const bidApi = {
  create: (data: { tenderId: number; items: { criteriaId: number; value: number; description?: string }[] }) =>
    apiClient.post<Bid>('/api/bids', data),
  getById: (id: number) =>
    apiClient.get<Bid>(`/api/bids/${id}`),
  submit: (id: number) =>
    apiClient.post<Bid>(`/api/bids/${id}/submit`),
  getMyBids: (params?: { page?: number; size?: number }) =>
    apiClient.get<Page<Bid>>('/api/bids/tenderer', { params }),
  getByTender: (tenderId: number, params?: { page?: number; size?: number }) =>
    apiClient.get<Page<Bid>>(`/api/bids/tender/${tenderId}`, { params }),
  uploadDocument: (bidId: number, file: FormData) =>
    apiClient.post(`/api/bids/${bidId}/documents`, file, { headers: { 'Content-Type': 'multipart/form-data' } }),
  // Seal
  getSealStatus: (bidId: number) =>
    apiClient.get(`/api/v1/bids/${bidId}/seal/status`),
  unsealAll: (tenderId: number) =>
    apiClient.post(`/api/v1/bids/tender/${tenderId}/unseal-all`),
  // Anti-collusion
  analyzeCollusion: (tenderId: number) =>
    apiClient.get(`/api/v1/anti-collusion/tender/${tenderId}/analyze`),
};

// ============================================================
// Evaluation API
// ============================================================
export const evaluationApi = {
  create: (tenderId: number, data: { bidId: number; criteriaScores: { criteriaId: number; score: number; justification?: string }[]; comments?: string }) =>
    apiClient.post<Evaluation>(`/api/evaluations/tenders/${tenderId}`, data),
  getByTender: (tenderId: number) =>
    apiClient.get<Evaluation[]>(`/api/evaluations/tenders/${tenderId}`),
  getById: (id: number) =>
    apiClient.get<Evaluation>(`/api/evaluations/${id}`),
  // Multi-criteria
  configureCategories: (tenderId: number, data: unknown[]) =>
    apiClient.post(`/api/multi-criteria/tenders/${tenderId}/categories`, data),
  getResults: (tenderId: number) =>
    apiClient.get(`/api/multi-criteria/tenders/${tenderId}/results`),
  // Conflict of interest
  declareConflict: (tenderId: number, data: { hasConflict: boolean; conflictDescription?: string }) =>
    apiClient.post(`/api/conflict-of-interest/tenders/${tenderId}/declare`, data),
};

// ============================================================
// Contract API
// ============================================================
export const contractApi = {
  create: (data: Partial<Contract>) =>
    apiClient.post<Contract>('/api/contracts', data),
  getById: (id: number) =>
    apiClient.get<Contract>(`/api/contracts/${id}`),
  getByTender: (tenderId: number) =>
    apiClient.get<Contract[]>(`/api/contracts/tender/${tenderId}`),
  activate: (id: number) =>
    apiClient.patch<Contract>(`/api/contracts/${id}/activate`),
  complete: (id: number) =>
    apiClient.patch<Contract>(`/api/contracts/${id}/complete`),
  // Performance
  submitPerformance: (contractId: number, data: unknown) =>
    apiClient.post(`/api/vendor-performance/contracts/${contractId}`, data),
  getPerformance: (contractId: number) =>
    apiClient.get(`/api/vendor-performance/contracts/${contractId}`),
  // Amendments
  requestAmendment: (contractId: number, data: unknown) =>
    apiClient.post(`/api/contracts/${contractId}/amendments`, data),
  getAmendments: (contractId: number) =>
    apiClient.get(`/api/contracts/${contractId}/amendments`),
};

// ============================================================
// User & Qualification API
// ============================================================
export const userApi = {
  getById: (id: number) =>
    apiClient.get(`/api/users/${id}`),
  getAll: (params?: { page?: number; size?: number }) =>
    apiClient.get('/api/users', { params }),
  // Vendor qualification
  submitQualification: (data: unknown) =>
    apiClient.post<VendorQualification>('/api/vendor-qualifications', data),
  getQualifications: (orgId: number) =>
    apiClient.get<VendorQualification[]>(`/api/vendor-qualifications/organization/${orgId}`),
  isQualified: (orgId: number) =>
    apiClient.get<boolean>(`/api/vendor-qualifications/organization/${orgId}/qualified`),
  // Blacklist
  isBlacklisted: (orgId: number) =>
    apiClient.get<boolean>(`/api/blacklist/organization/${orgId}/status`),
};

// ============================================================
// Reports API
// ============================================================
export const reportApi = {
  getProcurementSummary: (from?: string, to?: string) =>
    apiClient.get('/api/reports/procurement-summary', { params: { from, to } }),
  getAuditActivity: (from?: string, to?: string) =>
    apiClient.get('/api/reports/audit-activity', { params: { from, to } }),
};
