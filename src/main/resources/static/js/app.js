    const fileInput = document.getElementById("files");
    const ownerSelect = document.getElementById("owner");
    const fileList = document.getElementById("file-list");
    const fileCount = document.getElementById("file-count");
    const problemList = document.getElementById("problem-list");
    const problemCount = document.getElementById("problem-count");
    const uploadButton = document.getElementById("upload-button");
    const clearButton = document.getElementById("clear-button");
    const progress = document.getElementById("progress");
    const uploadProgress = document.getElementById("upload-progress");
    const message = document.getElementById("message");
    const destination = document.getElementById("destination");
    const selectedFiles = [];
    const problemFiles = [];
    let uploading = false;
    let ownersAvailable = false;

    function updateButtons() {
        uploadButton.disabled = uploading || !ownerSelect.value || selectedFiles.length === 0;
        clearButton.disabled = uploading || (selectedFiles.length === 0 && problemFiles.length === 0);
        fileInput.disabled = uploading;
        ownerSelect.disabled = uploading || !ownersAvailable;
    }

    function renderList(items, container, allowRemove) {
        container.replaceChildren();

        for (const [index, item] of items.entries()) {
            const row = document.createElement("li");
            row.className = "file-item";

            row.append(createPreview(item));

            const info = document.createElement("div");
            info.className = "file-info";

            const path = document.createElement("div");
            path.className = "file-path";
            path.textContent = item.file.webkitRelativePath || item.file.name;

            const status = document.createElement("div");
            status.className = "file-status";
            status.textContent = `${formatSize(item.file.size)} · ${item.status}`;
            if (item.error) status.textContent += ` — ${item.error}`;
            info.append(path, status);

            row.append(info);
            if (allowRemove) {
                const remove = document.createElement("button");
                remove.className = "remove-button";
                remove.type = "button";
                remove.textContent = "Quitar";
                remove.setAttribute("aria-label", `Quitar ${item.file.name}`);
                remove.disabled = uploading;
                remove.addEventListener("click", () => {
                    releasePreview(item);
                    items.splice(index, 1);
                    renderFiles();
                });
                row.append(remove);
            }
            container.append(row);
        }
    }

    function renderFiles() {
        fileCount.textContent = `(${selectedFiles.length})`;
        problemCount.textContent = `(${problemFiles.length})`;
        renderList(selectedFiles, fileList, true);
        renderList(problemFiles, problemList, false);
        updateButtons();
    }

    function formatSize(bytes) {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    function getPreviewUrl(item) {
        if (!item.previewUrl) {
            item.previewUrl = URL.createObjectURL(item.file);
        }
        return item.previewUrl;
    }

    function releasePreview(item) {
        if (item.previewUrl) {
            URL.revokeObjectURL(item.previewUrl);
            item.previewUrl = null;
        }
    }

    function createPreview(item) {
        const preview = document.createElement("div");
        preview.className = "file-preview";

        const type = item.file.type || "";
        if (type.startsWith("image/")) {
            const image = document.createElement("img");
            image.src = getPreviewUrl(item);
            image.alt = `Vista previa de ${item.file.name}`;
            preview.append(image);
        } else if (type.startsWith("video/")) {
            const video = document.createElement("video");
            video.src = getPreviewUrl(item);
            video.muted = true;
            video.preload = "metadata";
            video.playsInline = true;
            video.setAttribute("aria-label", `Vista previa de ${item.file.name}`);
            preview.append(video);
        } else {
            preview.textContent = "Sin vista previa";
        }
        return preview;
    }

    fileInput.addEventListener("change", () => {
        for (const file of fileInput.files) {
            selectedFiles.push({ file, status: "Pendiente" });
        }
        fileInput.value = "";
        uploadProgress.value = 0;
        progress.textContent = "0%";
        message.textContent = "";
        renderFiles();
    });

    ownerSelect.addEventListener("change", () => {
        destination.textContent = ownerSelect.value
            ? `Destino: ${ownerSelect.value}/Photos/{año} dentro de la raíz configurada.`
            : "La carpeta de destino se determina con el usuario seleccionado.";
        updateButtons();
    });

    clearButton.addEventListener("click", () => {
        for (const item of [...selectedFiles, ...problemFiles]) {
            releasePreview(item);
        }
        selectedFiles.length = 0;
        problemFiles.length = 0;
        uploadProgress.value = 0;
        progress.textContent = "0%";
        message.textContent = "";
        renderFiles();
    });

    function updateProgress(completed, activeProgress, total, status) {
        const percent = total === 0 ? 0 : Math.min(100, Math.floor((completed + activeProgress) * 100 / total));
        uploadProgress.value = percent;
        progress.textContent = `${percent}% · ${status}`;
    }

    function sendFile(item, owner, onProgress) {
        const form = new FormData();
        form.append("images", item.file, item.file.name);
        form.append("modifiedDate", new Date(item.file.lastModified).toISOString());
        form.append("owner", owner);

        return new Promise((resolve, reject) => {
            const request = new XMLHttpRequest();
            request.open("POST", "/cargaImagenes/upload");
            request.upload.addEventListener("progress", event => {
                if (event.lengthComputable) onProgress(event.loaded / event.total);
            });
            request.addEventListener("load", () => {
                if (request.status >= 200 && request.status < 300) {
                    resolve();
                } else {
                    const error = new Error(request.responseText || `Error HTTP ${request.status}`);
                    error.status = request.status;
                    reject(error);
                }
            });
            request.addEventListener("error", () => reject(new Error("Error de red durante la carga.")));
            request.addEventListener("abort", () => reject(new Error("La carga fue interrumpida.")));
            request.send(form);
        });
    }

    uploadButton.addEventListener("click", async () => {
        const initialQueue = [...selectedFiles];
        const owner = ownerSelect.value;
        const total = initialQueue.length;
        let uploaded = 0;
        let cancelled = 0;
        let skippedDuplicates = 0;
        let retryQueue = [];
        let activeProgress = 0;

        uploading = true;
        message.className = "message";
        message.textContent = "";
        uploadProgress.value = 0;
        renderFiles();

        try {
            for (const [index, item] of initialQueue.entries()) {
                item.status = "Cargando (intento 1 de 3)";
                item.error = "";
                renderFiles();

                try {
                    await sendFile(item, owner, fraction => {
                        activeProgress = fraction;
                        updateProgress(uploaded + cancelled + skippedDuplicates, activeProgress, total,
                            `Carga inicial ${index + 1}/${total}: ${item.file.name}`);
                    });
                    uploaded++;
                    releasePreview(item);
                    selectedFiles.splice(selectedFiles.indexOf(item), 1);
                } catch (error) {
                    item.error = error.message;
                    selectedFiles.splice(selectedFiles.indexOf(item), 1);
                    problemFiles.push(item);
                    if (error.status === 409) {
                        item.status = "Omitido: ya existe (sin reintento)";
                        skippedDuplicates++;
                    } else {
                        item.status = "Pendiente de reintento";
                        retryQueue.push(item);
                    }
                }
                activeProgress = 0;
                updateProgress(uploaded + cancelled + skippedDuplicates, activeProgress, total,
                    `Carga inicial ${index + 1}/${total}`);
                renderFiles();
            }

            for (let attempt = 2; attempt <= 3 && retryQueue.length > 0; attempt++) {
                const nextRetryQueue = [];
                for (const [index, item] of retryQueue.entries()) {
                    item.attempts = attempt;
                    item.status = `Reintento ${attempt - 1}/2 en curso`;
                    renderFiles();

                    try {
                        await sendFile(item, owner, fraction => {
                            activeProgress = fraction;
                            updateProgress(uploaded + cancelled + skippedDuplicates, activeProgress, total,
                                `Reintento ${attempt - 1}/2 ${index + 1}/${retryQueue.length}: ${item.file.name}`);
                        });
                        uploaded++;
                        releasePreview(item);
                        problemFiles.splice(problemFiles.indexOf(item), 1);
                    } catch (error) {
                        item.error = error.message;
                        if (error.status === 409) {
                            item.status = "Omitido: ya existe (sin reintento)";
                            skippedDuplicates++;
                        } else if (attempt === 3) {
                            item.status = "Cancelado tras 3 intentos";
                            cancelled++;
                        } else {
                            item.status = `Falló el intento ${attempt}; se reintentará`;
                            nextRetryQueue.push(item);
                        }
                    }
                    activeProgress = 0;
                    updateProgress(uploaded + cancelled + skippedDuplicates, activeProgress, total,
                        `Reintento ${attempt - 1}/2 ${index + 1}/${retryQueue.length}`);
                    renderFiles();
                }
                retryQueue = nextRetryQueue;
            }

            let summaryError = "";
            try {
                const summaryForm = new FormData();
                summaryForm.append("owner", owner);
                summaryForm.append("uploadedCount", String(uploaded));
                const response = await fetch("/cargaImagenes/upload/complete", {
                    method: "POST",
                    body: summaryForm
                });
                if (!response.ok) {
                    const detail = await response.text();
                    throw new Error(detail || `Error HTTP ${response.status}`);
                }
            } catch (error) {
                summaryError = error.message;
            }

            uploadProgress.value = 100;
            progress.textContent = `100% · ${uploaded} cargado(s), ${skippedDuplicates} duplicado(s), ${cancelled} cancelado(s)`;
            if (summaryError) {
                message.textContent = `La carga terminó, pero no se pudo registrar el resumen: ${summaryError}`;
                message.className = "message error";
            } else if (cancelled === 0 && skippedDuplicates === 0) {
                message.textContent = "Todos los archivos se cargaron correctamente.";
                message.className = "message success";
            } else {
                const issueSummary = [];
                if (skippedDuplicates > 0) {
                    issueSummary.push(`${skippedDuplicates} duplicado(s) omitido(s) sin reintento`);
                }
                if (cancelled > 0) {
                    issueSummary.push(`${cancelled} archivo(s) cancelado(s) tras 3 intentos`);
                }
                message.textContent = `${issueSummary.join("; ")}. Consulta la lista de problemas.`;
                message.className = "message error";
            }
        } finally {
            uploading = false;
            renderFiles();
        }
    });

    async function loadOwners() {
        try {
            const response = await fetch("/cargaImagenes/owners");
            if (!response.ok) throw new Error(`Error HTTP ${response.status}`);
            const owners = await response.json();

            ownerSelect.replaceChildren(new Option("Selecciona un usuario", ""));
            for (const owner of owners) {
                ownerSelect.add(new Option(owner, owner));
            }
            ownersAvailable = owners.length > 0;
            if (owners.length === 0) {
                message.textContent = "No se encontraron carpetas de usuario en el directorio configurado.";
                message.className = "message error";
            }
            updateButtons();
        } catch (error) {
            ownersAvailable = false;
            ownerSelect.replaceChildren(new Option("No se pudieron cargar los usuarios", ""));
            message.textContent = `Error al consultar las carpetas de usuario: ${error.message}`;
            message.className = "message error";
            updateButtons();
        }
    }

    loadOwners();
