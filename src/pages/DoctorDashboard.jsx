import React from "react";
import { Grid, Container, Box } from "@mui/material";
import AppointmentCalendar from "../components/AppointmentCalendar";
import PatientList from "../components/PatientList";
import Statistics from "../components/Statistics";
import Notifications from "../components/Notifications";

const DoctorDashboard = () => {
  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Grid container spacing={3}>
        {/* Sol Bölüm */}
        <Grid item xs={12} md={8}>
          <Box sx={{ mb: 3 }}>
            <AppointmentCalendar />
          </Box>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <PatientList />
            </Grid>
            <Grid item xs={12} md={6}>
              <Statistics />
            </Grid>
          </Grid>
        </Grid>

        {/* Sağ Bölüm */}
        <Grid item xs={12} md={4}>
          <Notifications />
        </Grid>
      </Grid>
    </Container>
  );
};

export default DoctorDashboard;
