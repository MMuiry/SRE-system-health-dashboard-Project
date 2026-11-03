# Incident report: High 5xx error rate on staging
Date of report: 2026/11/03

Detection time: 2026-11-03 23:03

## Detection method
Spikes in the Grafana Error Rate panel spiked from 0% to 25%, 
this also fired an alert in prometheus. 

## Investigation from logs
Investigating the logs shows it started at 11:01 and it comes
from the endpoint /simulate-error. This is returning 500 status.
This is from the app system-health and in the environment staging

## root cause
Simulate-error is normally disabled as an endpoint but the 
environment variable "ENABLE_FAULT_INJECTION" was enabled 
causing it to return 500 status

## Recovery actions
Looking at the git commits, we pin pointed the change was 
a test run of the ENABLE_FAULT_INJECTION, one line was reverted
back to what it was previous to the change. CI/CD deployed
the fixed build and no issues were seen

## Prevention ideas
add additional checks in the jenkins to check for this
environmental variable is always off for prod as well as 
staging unless specified otherwise during the smoke tests