import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import AppBar from '@mui/material/AppBar'
import { Link, Outlet, useLocation } from 'react-router-dom'

const sections = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Admin', path: '/admin' },
]

export default function AppLayout() {
  const { pathname } = useLocation()

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Dundie Awards
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {sections.map((section) => (
              <Button
                key={section.path}
                component={Link}
                to={section.path}
                color="inherit"
                variant={pathname.startsWith(section.path) ? 'outlined' : 'text'}
              >
                {section.label}
              </Button>
            ))}
          </Box>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </>
  )
}
