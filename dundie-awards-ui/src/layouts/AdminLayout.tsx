import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import Typography from '@mui/material/Typography'
import { Link, Outlet, useLocation } from 'react-router-dom'

const tabs = [
  { label: 'Organizations', path: '/admin/organizations' },
  { label: 'Employees', path: '/admin/employees' },
  { label: 'Award log', path: '/admin/awards' },
  { label: 'Activities', path: '/admin/activities' },
]

export default function AdminLayout() {
  const { pathname } = useLocation()
  const activeTab = tabs.find((tab) => pathname.startsWith(tab.path))?.path ?? tabs[0].path

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Admin
      </Typography>

      <Tabs value={activeTab} sx={{ mb: 2 }}>
        {tabs.map((tab) => (
          <Tab key={tab.path} label={tab.label} value={tab.path} to={tab.path} component={Link} />
        ))}
      </Tabs>

      <Outlet />
    </>
  )
}
