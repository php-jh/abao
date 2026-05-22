# 阿里云服务器部署流程

## 1. 准备阿里云服务

需要开通：

- ECS 云服务器
- 智能语音交互 NLS
- 一句话识别 / 录音识别相关能力

在阿里云智能语音交互控制台准备：

- `ALIYUN_NLS_APPKEY`
- `ALIYUN_NLS_TOKEN`

当前项目第一阶段使用 REST 一句话识别接口：

- 音频格式：`wav`
- 采样率：`16000`
- 语言：英文录音也可识别，但效果取决于阿里云模型和控制台配置

## 2. 购买 ECS

建议配置：

- 地域：华东 2 上海，和 NLS 网关一致
- 系统：Ubuntu 22.04 LTS
- 配置：2 核 2G 起步
- 带宽：3M 起步
- 安全组开放：`22`、`80`、`443`

## 3. 连接服务器

```bash
ssh root@你的服务器公网IP
```

## 4. 安装 Node.js

```bash
apt update
apt install -y curl
curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
apt install -y nodejs
node -v
npm -v
```

## 5. 上传项目

可以用 SFTP、宝塔、WinSCP，或在本机执行：

```bash
scp -r ./外包 root@你的服务器公网IP:/opt/po-speaking
```

服务器上进入目录：

```bash
cd /opt/po-speaking
```

## 6. 配置阿里云语音识别环境变量

```bash
cat > .env <<'EOF'
ALIYUN_NLS_APPKEY=你的AppKey
ALIYUN_NLS_TOKEN=你的Token
PORT=5173
EOF
```

临时启动时也可以这样：

```bash
export ALIYUN_NLS_APPKEY="你的AppKey"
export ALIYUN_NLS_TOKEN="你的Token"
export PORT=5173
node server.cjs
```

## 7. 使用 PM2 常驻运行

```bash
npm install -g pm2
cd /opt/po-speaking
pm2 start server.cjs --name po-speaking --update-env
pm2 save
pm2 startup
```

如果使用 `.env`，可以先加载：

```bash
set -a
source .env
set +a
pm2 restart po-speaking --update-env
```

## 8. 安装 Nginx

```bash
apt install -y nginx
```

新建配置：

```bash
cat > /etc/nginx/sites-available/po-speaking <<'EOF'
server {
    listen 80;
    server_name 你的域名;

    client_max_body_size 20m;

    location / {
        proxy_pass http://127.0.0.1:5173;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

ln -s /etc/nginx/sites-available/po-speaking /etc/nginx/sites-enabled/po-speaking
nginx -t
systemctl reload nginx
```

## 9. 配置 HTTPS

浏览器麦克风权限要求正式域名必须使用 HTTPS。

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d 你的域名
```

## 10. 测试接口

打开：

```text
https://你的域名
```

测试流程：

1. 点击进入系统
2. 进入课中演练
3. 点击开始录入
4. 说英文
5. 再点停止并识别
6. 学生回答文本框应出现识别结果

## 11. 常见问题

### 麦克风无法使用

正式部署必须 HTTPS。仅 `localhost` 可以无 HTTPS 使用麦克风。

### 返回 ALIYUN_NOT_CONFIGURED

服务器没有配置：

- `ALIYUN_NLS_APPKEY`
- `ALIYUN_NLS_TOKEN`

### 返回 ALIYUN_ASR_FAILED

检查：

- Token 是否过期
- AppKey 是否正确
- 智能语音交互服务是否开通
- ECS 是否能访问 `https://nls-gateway-cn-shanghai.aliyuncs.com`

### Token 过期

当前第一阶段使用手动配置 Token。正式上线建议增加后端自动刷新 Token。

---

# Windows Server 2022 部署流程

适用环境：

- Windows Server 2022 数据中心版 64 位中文版
- 阿里云 ECS
- 域名已解析到服务器公网 IP
- 使用阿里云免费 SSL 证书

推荐架构：

```text
学生浏览器
  -> https://你的域名
  -> IIS HTTPS 站点
  -> 反向代理到 http://127.0.0.1:5173
  -> Node server.cjs
  -> 阿里云语音识别接口
```

## Windows 1. 安装 Node.js

下载并安装 Node.js LTS：

```text
https://nodejs.org/
```

安装完成后打开 PowerShell：

```powershell
node -v
npm -v
```

能看到版本号即可。

## Windows 2. 上传项目

建议放到：

```text
C:\po-speaking
```

目录结构应类似：

```text
C:\po-speaking
  index.html
  styles.css
  script.js
  server.cjs
  assets\
```

## Windows 3. 配置阿里云环境变量

先用 PowerShell 临时测试：

```powershell
cd C:\po-speaking
$env:ALIYUN_NLS_APPKEY="nOBt4SZhSXTB7ynx"
$env:ALIYUN_NLS_TOKEN="你的Token"
$env:PORT="5173"
node server.cjs
```

访问：

```text
http://127.0.0.1:5173
```

如果页面能打开，说明 Node 服务正常。

永久环境变量：

```powershell
setx ALIYUN_NLS_APPKEY "nOBt4SZhSXTB7ynx" /M
setx ALIYUN_NLS_TOKEN "你的Token" /M
setx PORT "5173" /M
```

设置后需要重新打开 PowerShell，或者重启服务器。

## Windows 4. 测试后端接口

PowerShell 测试：

```powershell
Invoke-WebRequest http://127.0.0.1:5173
```

如果阿里云环境变量没配好，测试 `/api/asr` 会返回：

```json
{
  "error": "ALIYUN_NOT_CONFIGURED"
}
```

这是正常提示，表示接口存在，只是还没配置阿里云参数。

## Windows 5. 安装 IIS

打开“服务器管理器”：

```text
添加角色和功能
  -> 服务器角色
  -> 勾选 Web 服务器(IIS)
  -> 安装
```

也可以 PowerShell 管理员执行：

```powershell
Install-WindowsFeature Web-Server -IncludeManagementTools
```

## Windows 6. 安装 IIS URL Rewrite 和 ARR

IIS 反向代理需要两个组件：

- URL Rewrite
- Application Request Routing，简称 ARR

安装后打开 IIS 管理器：

```text
服务器节点
  -> Application Request Routing Cache
  -> Server Proxy Settings
  -> 勾选 Enable proxy
  -> Apply
```

## Windows 7. 申请阿里云免费 SSL 证书

阿里云控制台：

```text
数字证书管理服务 / SSL 证书
  -> 免费证书
  -> 创建证书
  -> 绑定域名
  -> DNS 验证
  -> 等待签发
```

证书签发后下载：

```text
服务器类型选择 IIS
```

通常会得到：

```text
xxx.pfx
pfx-password.txt
```

## Windows 8. IIS 导入证书

打开 IIS 管理器：

```text
服务器节点
  -> 服务器证书
  -> 导入
  -> 选择 .pfx 文件
  -> 输入 pfx-password.txt 里的密码
```

## Windows 9. IIS 新建 HTTPS 网站

可以建一个空网站，只用来做代理。

网站物理路径例如：

```text
C:\inetpub\po-speaking
```

绑定：

```text
类型：https
IP：全部未分配
端口：443
主机名：你的域名
SSL 证书：选择刚导入的证书
```

同时可绑定 80 端口，用于跳转 HTTPS。

## Windows 10. IIS 配置反向代理到 Node

在网站根目录创建 `web.config`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <rewrite>
      <rules>
        <rule name="ReverseProxyToNode" stopProcessing="true">
          <match url="(.*)" />
          <action type="Rewrite" url="http://127.0.0.1:5173/{R:1}" />
        </rule>
      </rules>
    </rewrite>
  </system.webServer>
</configuration>
```

然后访问：

```text
https://你的域名
```

应能看到系统首页。

## Windows 11. 后台运行 Node 服务

推荐用 NSSM 注册 Windows 服务，比在窗口里直接运行稳定。

下载 NSSM：

```text
https://nssm.cc/download
```

假设放到：

```text
C:\tools\nssm\nssm.exe
```

管理员 PowerShell：

```powershell
C:\tools\nssm\nssm.exe install PoSpeaking
```

弹窗中填写：

```text
Path: C:\Program Files\nodejs\node.exe
Startup directory: C:\po-speaking
Arguments: server.cjs
```

在 Environment 里添加：

```text
ALIYUN_NLS_APPKEY=你的AppKey
ALIYUN_NLS_TOKEN=你的Token
PORT=5173
```

启动服务：

```powershell
Start-Service PoSpeaking
```

查看状态：

```powershell
Get-Service PoSpeaking
```

## Windows 12. 防火墙和安全组

阿里云安全组放行：

```text
80
443
```

Windows 防火墙放行：

```powershell
New-NetFirewallRule -DisplayName "HTTP 80" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
New-NetFirewallRule -DisplayName "HTTPS 443" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow
```

不建议对公网开放 `5173`。让它只跑在本机，给 IIS 反向代理即可。

## Windows 13. 测试课堂功能

学生访问：

```text
https://你的域名
```

测试：

1. 点击进入系统
2. 点击课中演练
3. 点击开始录入
4. 浏览器弹出麦克风权限，选择允许
5. 说英文
6. 点击停止并识别
7. 学生回答文本框出现识别结果

## Windows 14. 注意事项

- HTTPS 是给浏览器麦克风权限用的。
- Node 后端调用阿里云接口本身不依赖你的域名 HTTPS。
- `ALIYUN_NLS_TOKEN` 会过期，正式上线建议做自动刷新 Token。
- 阿里云 Key 不要写进 `script.js`，只能放服务端环境变量。
- 如果换证书，需要在 IIS 重新导入并绑定。
