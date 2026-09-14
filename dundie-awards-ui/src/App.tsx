import CssBaseline from '@mui/material/CssBaseline'
import { Navigate, Route, Routes } from 'react-router-dom'
import ActivitiesPage from './features/activities/ActivitiesPage'
import AwardsPage from './features/awards/AwardsPage'
import DashboardPage from './features/awards/DashboardPage'
import EmployeesPage from './features/employees/EmployeesPage'
import OrganizationsPage from './features/organizations/OrganizationsPage'
import AdminLayout from './layouts/AdminLayout'
import AppLayout from './layouts/AppLayout'

function App() {
  return (
    <>
      <CssBaseline />
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<Navigate to="/admin/organizations" replace />} />
            <Route path="organizations" element={<OrganizationsPage />} />
            <Route path="employees" element={<EmployeesPage />} />
            <Route path="awards" element={<AwardsPage />} />
            <Route path="activities" element={<ActivitiesPage />} />
          </Route>
          {/* unknown paths, including the old /admin/awards, land on the dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
