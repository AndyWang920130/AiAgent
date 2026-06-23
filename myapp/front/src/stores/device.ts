import { ref } from 'vue'

export type DeviceStatus = 'active' | 'inactive' | 'maintenance'

export interface Device {
  id: number
  serialNumber: string
  model: string
  status: DeviceStatus
  outgoingDate: string
  installationDate: string
  warrantyYears: number
  contactPerson: string
  contactPhone: string
  latitude: number | null
  longitude: number | null
  address: string
}

export const devices = ref<Device[]>([
  {
    id: 1,
    serialNumber: 'SN-2025-001',
    model: 'SmartSensor Pro X1',
    status: 'active',
    outgoingDate: '2025-01-10',
    installationDate: '2025-01-15',
    warrantyYears: 3,
    contactPerson: '张伟',
    contactPhone: '13800138001',
    latitude: 39.9042,
    longitude: 116.4074,
    address: '北京市朝阳区建国路89号',
  },
  {
    id: 2,
    serialNumber: 'SN-2025-002',
    model: 'DataHub Plus 200',
    status: 'maintenance',
    outgoingDate: '2025-02-05',
    installationDate: '2025-02-20',
    warrantyYears: 2,
    contactPerson: '李娜',
    contactPhone: '13900139002',
    latitude: 31.2304,
    longitude: 121.4737,
    address: '上海市浦东新区世纪大道1号',
  },
  {
    id: 3,
    serialNumber: 'SN-2024-087',
    model: 'EnviroMonitor E3',
    status: 'active',
    outgoingDate: '2024-11-01',
    installationDate: '2024-11-10',
    warrantyYears: 5,
    contactPerson: '王芳',
    contactPhone: '13700137003',
    latitude: 23.1291,
    longitude: 113.2644,
    address: '广州市天河区珠江新城花城大道85号',
  },
  {
    id: 4,
    serialNumber: 'SN-2024-063',
    model: 'IndustrialGate G10',
    status: 'inactive',
    outgoingDate: '2024-09-15',
    installationDate: '2024-10-01',
    warrantyYears: 1,
    contactPerson: '陈明',
    contactPhone: '13600136004',
    latitude: 30.5728,
    longitude: 104.0668,
    address: '成都市武侯区天府大道北段1700号',
  },
  {
    id: 5,
    serialNumber: 'SN-2025-039',
    model: 'SmartSensor Pro X2',
    status: 'active',
    outgoingDate: '2025-03-20',
    installationDate: '2025-04-01',
    warrantyYears: 3,
    contactPerson: '刘洋',
    contactPhone: '13500135005',
    latitude: 22.5431,
    longitude: 114.0579,
    address: '深圳市南山区科技园南区高新南七道',
  },
])

let nextId = 6

export function addDevice(device: Omit<Device, 'id'>) {
  devices.value.push({ ...device, id: nextId++ })
}

export function updateDevice(id: number, data: Partial<Omit<Device, 'id'>>) {
  const idx = devices.value.findIndex(d => d.id === id)
  if (idx !== -1) {
    devices.value[idx] = { ...devices.value[idx], ...data }
  }
}

export function deleteDevice(id: number) {
  devices.value = devices.value.filter(d => d.id !== id)
}

export function getDevice(id: number): Device | undefined {
  return devices.value.find(d => d.id === id)
}
