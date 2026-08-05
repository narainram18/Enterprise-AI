const email = "narainram123456789@gmail.com";
const password = "2005Narain@";

fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
})
.then(async r => {
    const data = await r.json();
    const token = data.data.token;
    console.log(`TOKEN: ${token}`);
    
    return fetch("http://localhost:8080/api/users/me", {
        headers: { "Authorization": `Bearer ${token}` }
    });
})
.then(async r => {
    const status = r.status;
    const body = await r.text();
    console.log(`ME STATUS: ${status}`);
    console.log(`ME BODY: ${body}`);
})
.catch(e => console.error(e));
