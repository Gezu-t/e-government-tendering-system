// ============================================================
// Core Domain Types for E-Government Tendering System
// ============================================================

// --- User & Auth ---
export type UserRole = 'TENDEREE' | 'TENDERER' | 'EVALUATOR' | 'COMMITTEE';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  organizations?: UserOrganization[];
}

export interface UserOrganization {
  organizationId: number;
  organizationName: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  role: UserRole;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegistrationRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  organization?: {
    name: string;
    registrationNumber: string;
    address?: string;
    contactPerson?: string;
    phone?: string;
    email?: string;
    organizationType: 'GOVERNMENT' | 'PRIVATE' | 'NGO';
  };
}

// --- Tender ---
export type TenderStatus = 'DRAFT' | 'PUBLISHED' | 'AMENDED' | 'CLOSED' | 'EVALUATION_IN_PROGRESS' | 'EVALUATED' | 'AWARDED' | 'CANCELLED';
export type TenderType = 'OPEN' | 'SELECTIVE' | 'LIMITED' | 'SINGLE';
export type AllocationStrategy = 'SINGLE' | 'COOPERATIVE' | 'COMPETITIVE';
export type CriteriaType = 'PRICE' | 'QUANTITY' | 'TIME' | 'QUALITY' | 'ENUMERATION' | 'EXPERIENCE';

export interface Tender {
  id: number;
  title: string;
  description: string;
  tendereeId: number;
  type: TenderType;
  status: TenderStatus;
  submissionDeadline: string;
  allocationStrategy: AllocationStrategy;
  minWinners?: number;
  maxWinners?: number;
  cutoffScore?: number;
  isAverageAllocation?: boolean;
  createdAt: string;
  updatedAt: string;
  criteria: TenderCriteria[];
  items: TenderItem[];
}

export interface TenderCriteria {
  id: number;
  name: string;
  description?: string;
  type: CriteriaType;
  weight: number;
  preferHigher?: boolean;
  active: boolean;
}

export interface TenderItem {
  id?: number;
  criteriaId: number;
  name: string;
  description?: string;
  quantity: number;
  unit?: string;
  estimatedPrice?: number;
}

export interface TenderAmendment {
  id: number;
  tenderId: number;
  amendmentNumber: number;
  reason: string;
  description?: string;
  previousDeadline?: string;
  newDeadline?: string;
  amendedBy: number;
  createdAt: string;
}

// --- Bid ---
export type BidStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_EVALUATION' | 'ACCEPTED' | 'REJECTED' | 'EVALUATED' | 'AWARDED' | 'CONTRACTED';

export interface Bid {
  id: number;
  tenderId: number;
  tendererId: number;
  tendererName?: string;
  status: BidStatus;
  totalPrice: number;
  submissionTime?: string;
  createdAt: string;
  items: BidItem[];
  documents: BidDocument[];
}

export interface BidItem {
  id: number;
  criteriaId: number;
  criteriaName?: string;
  value: number;
  description?: string;
}

export interface BidDocument {
  id: number;
  name: string;
  filePath: string;
  fileType?: string;
  fileSize?: number;
  createdAt: string;
}

// --- Evaluation ---
export type EvaluationStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface Evaluation {
  id: number;
  tenderId: number;
  bidId: number;
  bidderName?: string;
  evaluatorId: number;
  status: EvaluationStatus;
  overallScore?: number;
  comments?: string;
  createdAt: string;
  criteriaScores: CriteriaScore[];
}

export interface CriteriaScore {
  id: number;
  criteriaId: number;
  criteriaName?: string;
  score: number;
  justification?: string;
}

// --- Contract ---
export type ContractStatus = 'DRAFT' | 'PENDING_SIGNATURE' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED' | 'CANCELLED';

export interface Contract {
  id: number;
  tenderId: number;
  bidderId: number;
  contractNumber: string;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  totalValue: number;
  status: ContractStatus;
  createdAt: string;
  items: ContractItem[];
  milestones: ContractMilestone[];
}

export interface ContractItem {
  id: number;
  name: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  totalPrice: number;
}

export interface ContractMilestone {
  id: number;
  title: string;
  description?: string;
  dueDate: string;
  paymentAmount: number;
  status: string;
  completedDate?: string;
}

// --- Pagination ---
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// --- Vendor Qualification ---
export type QualificationStatus = 'PENDING' | 'UNDER_REVIEW' | 'QUALIFIED' | 'CONDITIONALLY_QUALIFIED' | 'DISQUALIFIED' | 'EXPIRED' | 'SUSPENDED';

export interface VendorQualification {
  id: number;
  organizationId: number;
  organizationName: string;
  qualificationCategory: string;
  status: QualificationStatus;
  qualificationScore?: number;
  validFrom?: string;
  validUntil?: string;
  createdAt: string;
}
