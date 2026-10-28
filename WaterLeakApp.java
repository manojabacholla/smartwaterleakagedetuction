
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class WaterLeakApp {

    private static final int PORT = 8080;
    private static final double LEAK_LIMIT = 10.0;
    private static final Path HISTORY_FILE = Path.of("readings.csv");

    private static final List<Reading> history = new ArrayList<>();

    private record Reading(
            String time,
            double flow,
            boolean leak
    ) {}

    public static void main(String[] args) throws Exception {
        loadHistory();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/", WaterLeakApp::handleRequest);
        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println("==================================");
        System.out.println(" SMART WATER LEAKAGE DETECTION");
        System.out.println("==================================");
        System.out.println("Server running at http://localhost:" + PORT);
        System.out.println("Leak threshold: " + LEAK_LIMIT + " L/min");
        System.out.println("History file: " + HISTORY_FILE.toAbsolutePath());
        System.out.println("Press Ctrl+C to stop the server.");

        server.start();
    }

    private static void handleRequest(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/")) {
                send(exchange, 200, "text/html; charset=UTF-8",
                        dashboard());
            } else if (path.equals("/api/simulate")) {
                simulate(exchange);
            } else if (path.equals("/api/readings")) {
                send(exchange, 200, "application/json; charset=UTF-8",
                        readingsJson());
            } else {
                send(exchange, 404, "text/plain; charset=UTF-8",
                        "Page not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "text/plain; charset=UTF-8",
                    "Error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private static synchronized void simulate(HttpExchange exchange)
            throws IOException {

        String query = exchange.getRequestURI().getRawQuery();
        String mode = "";

        if (query != null) {
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && pair[0].equals("mode")) {
                    mode = URLDecoder.decode(
                            pair[1], StandardCharsets.UTF_8);
                }
            }
        }

        double flow;

        if (mode.equalsIgnoreCase("leak")) {
            flow = 18.0 + Math.random() * 7.0;
        } else if (mode.equalsIgnoreCase("normal")) {
            flow = 2.0 + Math.random() * 5.0;
        } else {
            send(exchange, 400, "application/json; charset=UTF-8",
                    "{\"error\":\"Use normal or leak mode\"}");
            return;
        }

        boolean leakDetected = flow > LEAK_LIMIT;

        String time = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Reading reading = new Reading(time, flow, leakDetected);
        history.add(0, reading);

        saveReading(reading);

        System.out.printf(
                "%s | Flow: %.2f L/min | Leak: %s%n",
                time, flow, leakDetected ? "YES" : "NO");

        send(exchange, 200, "application/json; charset=UTF-8",
                "{\"time\":\"" + time
                + "\",\"flow\":" + flow
                + ",\"leak\":" + leakDetected
                + "}");
    }

    private static synchronized String readingsJson() {
        StringBuilder json = new StringBuilder("[");
        int count = Math.min(history.size(), 100);

        for (int i = 0; i < count; i++) {
            Reading r = history.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{\"time\":\"")
                .append(r.time())
                .append("\",\"flow\":")
                .append(r.flow())
                .append(",\"leak\":")
                .append(r.leak())
                .append("}");
        }

        return json.append("]").toString();
    }

    private static synchronized void saveReading(Reading reading)
            throws IOException {

        String line = reading.time() + ","
                + reading.flow() + ","
                + reading.leak() + System.lineSeparator();

        Files.writeString(
                HISTORY_FILE,
                line,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    private static synchronized void loadHistory()
            throws IOException {

        if (!Files.exists(HISTORY_FILE)) {
            return;
        }

        for (String line : Files.readAllLines(HISTORY_FILE)) {
            String[] parts = line.split(",");

            if (parts.length == 3) {
                try {
                    history.add(new Reading(
                            parts[0],
                            Double.parseDouble(parts[1]),
                            Boolean.parseBoolean(parts[2])));
                } catch (NumberFormatException ignored) {
                    // Skip invalid records.
                }
            }
        }

        history.sort((a, b) -> b.time().compareTo(a.time()));
        System.out.println("Loaded " + history.size()
                + " previous readings.");
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String contentType,
            String content) throws IOException {

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", contentType);
        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store");

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String dashboard() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Smart Water Leakage Detection</title>
<style>
*{box-sizing:border-box}
body{margin:0;font-family:Arial,sans-serif;background:#f1f5f9;color:#172554}
header{background:#0f3d65;color:white;padding:25px 7%}
header h1{margin:0 0 8px;font-size:27px}
header p{margin:0;color:#dbeafe}
main{max-width:1050px;margin:30px auto;padding:0 18px}
.status{background:white;padding:22px;border-radius:14px;margin-bottom:20px;
box-shadow:0 4px 14px #0000000b}
.status h2{margin-top:0}
#alert{font-weight:bold;font-size:19px;color:#64748b}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px}
.card{background:white;padding:22px;border-radius:14px;
box-shadow:0 4px 14px #0000000b}
.card h3{margin:0;color:#64748b;font-size:14px}
.value{font-size:29px;font-weight:bold;margin-top:14px}
.buttons{display:flex;gap:12px;flex-wrap:wrap;margin:22px 0}
button{border:0;padding:13px 20px;border-radius:8px;color:white;
font-weight:bold;cursor:pointer;font-size:15px}
.normal{background:#16835b}.leak{background:#dc3545}.refresh{background:#2563eb}
button:hover{filter:brightness(.92)}
.tablebox{background:white;padding:20px;border-radius:14px;overflow-x:auto}
table{border-collapse:collapse;width:100%;margin-top:15px}
th,td{text-align:left;padding:12px;border-bottom:1px solid #e2e8f0}
th{background:#eff6ff}
.bad{color:#dc2626;font-weight:bold}.good{color:#16835b;font-weight:bold}
.note{color:#64748b;font-size:13px;margin-top:22px}
@media(max-width:500px){header h1{font-size:22px}.value{font-size:25px}}
</style>
</head>
<body>
<header>
<h1>💧 Smart Water Leakage Detection</h1>
<p>Monitor flow readings and identify possible water leaks</p>
</header>
<main>
<section class="status">
<h2>System Status</h2>
<div id="alert">Waiting for your first reading...</div>
<p>Leak alert threshold: greater than 10 litres per minute</p>
</section>

<section class="cards">
<div class="card">
<h3>Current Water Flow</h3>
<div class="value" id="flow">-- L/min</div>
</div>
<div class="card">
<h3>Leak Detection</h3>
<div class="value" id="leakStatus">--</div>
</div>
<div class="card">
<h3>Total Readings</h3>
<div class="value" id="total">0</div>
</div>
</section>

<div class="buttons">
<button class="normal" onclick="simulate('normal')">Normal Flow</button>
<button class="leak" onclick="simulate('leak')">Simulate Water Leak</button>
<button class="refresh" onclick="refreshHistory()">Refresh History</button>
</div>

<section class="tablebox">
<h2>Recent Reading History</h2>
<table>
<thead><tr><th>Date and Time</th><th>Flow (L/min)</th>
<th>Result</th></tr></thead>
<tbody id="rows">
<tr><td colspan="3">No readings yet. Click a simulation button.</td></tr>
</tbody>
</table>
<p class="note">Readings are saved locally in readings.csv. This is a software
simulation, not a physical pipe sensor.</p>
</section>
</main>
<script>
async function simulate(mode){
 try{
  const response=await fetch('/api/simulate?mode='+mode);
  if(!response.ok) throw new Error(await response.text());
  const r=await response.json();
  showCurrent(r);
  await refreshHistory();
 }catch(e){alert('Could not record reading: '+e.message);}
}
function showCurrent(r){
 document.getElementById('flow').textContent=Number(r.flow).toFixed(2)+' L/min';
 const status=document.getElementById('leakStatus');
 const alertBox=document.getElementById('alert');
 if(r.leak){
  status.textContent='LEAK DETECTED';
  status.style.color='#dc2626';
  alertBox.textContent='⚠️ WARNING: Unusually high water flow detected!';
  alertBox.style.color='#dc2626';
 }else{
  status.textContent='NORMAL';
  status.style.color='#16835b';
  alertBox.textContent='✓ Water flow is within the normal range.';
  alertBox.style.color='#16835b';
 }
}
async function refreshHistory(){
 try{
  const response=await fetch('/api/readings');
  const data=await response.json();
  document.getElementById('total').textContent=data.length;
  const rows=document.getElementById('rows');
  if(data.length===0){
   rows.innerHTML='<tr><td colspan="3">No readings yet. Click a simulation button.</td></tr>';
   return;
  }
  rows.innerHTML='';
  data.forEach(r=>{
   const tr=document.createElement('tr');
   const time=document.createElement('td');
   const flow=document.createElement('td');
   const result=document.createElement('td');
   time.textContent=r.time;
   flow.textContent=Number(r.flow).toFixed(2);
   result.textContent=r.leak?'LEAK DETECTED':'NORMAL';
   result.className=r.leak?'bad':'good';
   tr.append(time,flow,result);
   rows.appendChild(tr);
  });
  if(data.length>0) showCurrent(data[0]);
 }catch(e){console.error(e);}
}
refreshHistory();
</script>
</body>
</html>
""";
    }
}

