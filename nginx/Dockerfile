# Config is baked into the image (not bind-mounted) so the stack also deploys correctly
# when Docker is driven from inside a Jenkins container.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
