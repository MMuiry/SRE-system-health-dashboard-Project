## What is the practical difference between Continuous Delivery and Continuous Deployment, and which would you choose for a payment service? Why?
Continuous delivery builds and performs smoke tests in staging environment. If it passes the tests it will ask for user approval to deploy in production.
Continuous deployment is when the approval step is skipped and will auto deploy to the production environment, it will still need to past the previous tests before deploying.
For a payment service, I would choose continuous delivery. This is because the payment service plays a vital role in the business that can have a large negotiate impact when errors occur with it such as;
Loss of sales if the payment service goes down, loss of reputation as customers may think the company is unreliable, Additional administation as confusing in payment may cause customer to think they ordered when they didn't
or accidentally order twice thinking it failed. Many of these are the impact can be an error in deploying to the service and would require a more strict approval procress.

## Which metric or log line gave you the quickest insight during the incident drill? What was missing?
The quickest insight was from the Error Rate, It allowed me to see when the incident has started and the impact on the current service. While logs would give me more info it wouldn't be the quickest as it would require the logs
to be analysed as well as finding when it started. To improve it, the IP of the user requesting would be beneficial as it allows us to see if it multiple users having issues, an attack or just one isolated user which would be useful
to see the scope of the incident. Theres also no notification set up for Prometheus so when the alert fired, no one would know unless there on Prometheus itself.

## How do metrics and logs complement each other? Give an example from your own project.
Metrics are time based meaning they will often be sent once every 1min  (or what ever the frequency set) and will include information about the system at that time which is useful as predicting issues before they happen. Logs are event driven
will produce timestamped logs whenever an event happens which in this case will be when a request comes through. Metrics was used in this project record how many requests each endpoint has had, the p95 of the latency, the app info, if the 
service is up and the uptime of the host, this is recorded every 15seconds. logs was used in this project to record the timestamps, message, logger name, level, method, endpoints, status environment and creates a log each time a request is made

## Which alert threshold would you tune before going live, and what trade-off are you managing (noise versus missed incidents)?
High error rate worked well in the drill but will have issues if only 2 requests come in during night time due to low traffic and one of them had an error which will result in a 50% error rate. To fix this I would implement a minimum amount 
threshold with the 5% minimum.

## What manual step remains in your workflow, and how would you automate or reduce its risk?
The monitoring services aren't deployed apart of the CD pipeline as they are not restarted when redeployment happens. This means if updates are required for the the monitoring tools or if the machine crashes, it will require a manual
docker-compose run on them. This could be automated by adding another script for deploying monitoring that can crate the network for the containers, start any containers thats not currently running and use /-/reload to reload the 
alert/prometheus.yml file without restarting the service. Grafana can pick up the changes without intervention by using updateIntervalSeconds: 30 in the dashboards.yml. all thats left is adding the step to the jenkins file
