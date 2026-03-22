import { useEffect, useState } from 'react';
import { List, Card, Typography, Space, Button, Spin, Flex, Tag, Badge, Empty } from 'antd';
import {
  BellOutlined, CheckOutlined, FileTextOutlined,
  TrophyOutlined, AuditOutlined, InfoCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { notificationApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { Notification, NotificationType } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

const { Title, Text } = Typography;

const typeConfig: Record<NotificationType, { color: string; icon: React.ReactNode }> = {
  TENDER_PUBLISHED: { color: 'green', icon: <FileTextOutlined /> },
  BID_RECEIVED: { color: 'blue', icon: <AuditOutlined /> },
  BID_EVALUATED: { color: 'cyan', icon: <AuditOutlined /> },
  CONTRACT_AWARDED: { color: 'gold', icon: <TrophyOutlined /> },
  SYSTEM: { color: 'default', icon: <InfoCircleOutlined /> },
  INFO: { color: 'blue', icon: <InfoCircleOutlined /> },
  WARNING: { color: 'orange', icon: <WarningOutlined /> },
};

export default function NotificationsPage() {
  const { userId } = useAuthStore();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  const fetchNotifications = async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const { data } = await notificationApi.getByUser(userId, filter === 'unread' ? true : undefined);
      setNotifications(Array.isArray(data) ? data : []);
    } catch {
      /* handled by interceptor */
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchNotifications();
  }, [userId, filter]);

  const markAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
    } catch {
      /* handled by interceptor */
    }
  };

  const markAllAsRead = async () => {
    const unread = notifications.filter((n) => !n.read);
    await Promise.all(unread.map((n) => notificationApi.markAsRead(n.id)));
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <Spin spinning={loading}>
      <Flex vertical gap={24} style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space>
            <BellOutlined style={{ fontSize: 24 }} />
            <Title level={3} style={{ margin: 0 }}>Notifications</Title>
            {unreadCount > 0 && <Badge count={unreadCount} />}
          </Space>
          <Space>
            <Button
              type={filter === 'all' ? 'primary' : 'default'}
              size="small"
              onClick={() => setFilter('all')}
            >All</Button>
            <Button
              type={filter === 'unread' ? 'primary' : 'default'}
              size="small"
              onClick={() => setFilter('unread')}
            >Unread</Button>
            {unreadCount > 0 && (
              <Button size="small" icon={<CheckOutlined />} onClick={markAllAsRead}>
                Mark all read
              </Button>
            )}
          </Space>
        </Space>

        <Card>
          <List
            dataSource={notifications}
            locale={{ emptyText: <Empty description="No notifications" /> }}
            renderItem={(item) => {
              const cfg = typeConfig[item.type] ?? typeConfig.INFO;
              return (
                <List.Item
                  style={{
                    background: item.read ? 'transparent' : '#f6ffed',
                    padding: '12px 16px',
                    borderRadius: 6,
                    marginBottom: 4,
                  }}
                  actions={
                    !item.read
                      ? [<Button type="link" size="small" icon={<CheckOutlined />} onClick={() => markAsRead(item.id)}>Read</Button>]
                      : undefined
                  }
                >
                  <List.Item.Meta
                    avatar={<span style={{ fontSize: 20 }}>{cfg.icon}</span>}
                    title={
                      <Space>
                        {!item.read && <Badge status="processing" />}
                        <Text strong={!item.read}>{item.title}</Text>
                        <Tag color={cfg.color}>{item.type.replace(/_/g, ' ')}</Tag>
                      </Space>
                    }
                    description={
                      <>
                        <Text>{item.message}</Text>
                        <br />
                        <Text type="secondary" style={{ fontSize: 12 }}>{dayjs(item.createdAt).fromNow()}</Text>
                      </>
                    }
                  />
                </List.Item>
              );
            }}
          />
        </Card>
      </Flex>
    </Spin>
  );
}
