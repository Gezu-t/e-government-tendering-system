import { useEffect, useState } from 'react';
import { Layout, Menu, Avatar, Dropdown, Typography, Space, Badge } from 'antd';
import {
  DashboardOutlined, FileTextOutlined, AuditOutlined, TeamOutlined,
  ContainerOutlined, LogoutOutlined, UserOutlined, BellOutlined,
  SafetyCertificateOutlined, BarChartOutlined, SettingOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { notificationApi } from '../../api/services';
import type { UserRole } from '../../types';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const roleMenuItems: Record<UserRole, { key: string; icon: React.ReactNode; label: string }[]> = {
  ADMIN: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/admin/audit', icon: <AuditOutlined />, label: 'Audit Log' },
    { key: '/admin/reports', icon: <BarChartOutlined />, label: 'Reports' },
  ],
  TENDEREE: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/tenders', icon: <FileTextOutlined />, label: 'My Tenders' },
    { key: '/tenders/create', icon: <FileTextOutlined />, label: 'Create Tender' },
    { key: '/evaluations', icon: <AuditOutlined />, label: 'Evaluations' },
    { key: '/contracts', icon: <ContainerOutlined />, label: 'Contracts' },
    { key: '/reports', icon: <BarChartOutlined />, label: 'Reports' },
  ],
  TENDERER: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/tenders/browse', icon: <FileTextOutlined />, label: 'Browse Tenders' },
    { key: '/bids', icon: <AuditOutlined />, label: 'My Bids' },
    { key: '/contracts', icon: <ContainerOutlined />, label: 'My Contracts' },
    { key: '/qualification', icon: <SafetyCertificateOutlined />, label: 'Qualification' },
  ],
  EVALUATOR: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/evaluations', icon: <AuditOutlined />, label: 'Evaluations' },
    { key: '/tenders/browse', icon: <FileTextOutlined />, label: 'Tenders' },
  ],
  COMMITTEE: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/evaluations', icon: <AuditOutlined />, label: 'Reviews' },
    { key: '/tenders/browse', icon: <FileTextOutlined />, label: 'Tenders' },
  ],
};

const adminItems = [
  { key: '/admin/users', icon: <TeamOutlined />, label: 'Users' },
  { key: '/admin/blacklist', icon: <SettingOutlined />, label: 'Blacklist' },
  { key: '/admin/audit', icon: <AuditOutlined />, label: 'Audit Log' },
  { key: '/admin/reports', icon: <BarChartOutlined />, label: 'Reports' },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userId, username, role, logout } = useAuthStore();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!userId) return;
    notificationApi.getUnreadCount(userId)
      .then(({ data }) => setUnreadCount(typeof data === 'number' ? data : 0))
      .catch(() => {});
    const interval = setInterval(() => {
      notificationApi.getUnreadCount(userId)
        .then(({ data }) => setUnreadCount(typeof data === 'number' ? data : 0))
        .catch(() => {});
    }, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  const menuItems = role ? roleMenuItems[role] || [] : [];

  const userMenu = {
    items: [
      { key: 'profile', icon: <UserOutlined />, label: 'Profile' },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined />, label: 'Logout', danger: true },
    ],
    onClick: ({ key }: { key: string }) => {
      if (key === 'logout') { logout(); navigate('/login'); }
    },
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} theme="dark" breakpoint="lg" collapsedWidth={80}>
        <div style={{ padding: '16px 24px', textAlign: 'center' }}>
          <Text strong style={{ color: '#fff', fontSize: 16 }}>E-Gov Tendering</Text>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
        {role === 'TENDEREE' && (
          <>
            <div style={{ padding: '8px 24px' }}>
              <Text style={{ color: '#ffffff80', fontSize: 11, textTransform: 'uppercase' }}>Admin</Text>
            </div>
            <Menu
              theme="dark"
              mode="inline"
              selectedKeys={[location.pathname]}
              items={adminItems}
              onClick={({ key }) => navigate(key)}
            />
          </>
        )}
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', borderBottom: '1px solid #f0f0f0' }}>
          <Space size="large">
            <Badge count={unreadCount}>
              <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} onClick={() => navigate('/notifications')} />
            </Badge>
            <Dropdown menu={userMenu} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1a365d' }} />
                <Text>{username}</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>({role})</Text>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8, minHeight: 360 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
