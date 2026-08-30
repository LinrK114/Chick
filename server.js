const express = require('express');
const path = require('path');

const app = express();
const PORT = 1111;

// 托管 public 目录下的静态文件
app.use(express.static(path.join(__dirname, 'public')));

// 修正：使用命名通配符 '/*splat' 替代 '*'
app.get('/*splat', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`✅ 服务运行在 http://localhost:${PORT}`);
});