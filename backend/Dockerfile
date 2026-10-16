# Backend Dockerfile (Node.js 20 Express)
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 4000
CMD ["node", "src/index.js"]
