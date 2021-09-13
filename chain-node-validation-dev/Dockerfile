FROM openjdk:11-stretch

ARG PROJ_VERSION
# Work around for docker not wishing to have build args persisted/used within the image
ENV PROJ_VERSION ${PROJ_VERSION}

#Debugging and VM
ENV JVM_DEBUG_PORT 9000
ENV JVM_MAX_MEMORY 4g
ENV EXTRA_JVM_OPTIONS ""

#General config
ENV CHAIN_ID 30
ENV NODE_ID 0
ENV MAX_TX_PER_BLOCK 100000
ENV HEALTH_USER jenson
ENV HEALTH_PASSWORD onepathMalta
ENV EXTRA_CMD_LINE_OPTIONS ""

#Vault config
ENV DEV_VAULT_FILE=/setlnet/dev_vault.yaml
ENV VAULT_NAME password-server
ENV VAULT_LISTEN_PORT 20565
ENV VAULT_SERVERS 20.20.200.42:20563

VOLUME /setlnet

WORKDIR /validation_node

COPY libs/pychain-node-validation-dev-${PROJ_VERSION}.jar .

# libcrypto 1.1
COPY native/v1.1 native

CMD if [ "x$DEV_VAULT_FILE" != "x" ] ; then export DEV_VAULT_LINE="-Dio.setl.passwd.dev_vault_file=${DEV_VAULT_FILE}" ; \
else export STD_VAULT_LINE="--setl.vault.name=${VAULT_NAME} --setl.vault.listen=${VAULT_LISTEN_PORT} --setl.vault.servers=${VAULT_SERVERS}" ;fi && \
exec java \
    -Dio.setl.bc.store.bos.useSparseFiles=true \
    -Djava.library.path=/validation_node/native \
    ${DEV_VAULT_LINE} \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${JVM_DEBUG_PORT} \
    -Xmx${JVM_MAX_MEMORY} \
    ${EXTRA_JVM_OPTIONS} \
    -jar pychain-node-validation-dev-${PROJ_VERSION}.jar \
    --spring.config.location=/setlnet/${CHAIN_ID}/setl${NODE_ID}.properties \
    --logging.config=/setlnet/${CHAIN_ID}/setl${NODE_ID}.log4j.xml \
    --transport=netty \
    --db-store.type=sqlite \
    --nostart=false \
    --demowallet=wallet-0.dat \
    --management.endpoint.health.enabled=true \
    --health.user=${HEALTH_USER} \
    ${STD_VAULT_LINE} \
    --health.password=${HEALTH_PASSWORD} \

    --vnode.setting.max-tx-per-block=${MAX_TX_PER_BLOCK} \
    ${EXTRA_CMD_LINE_OPTIONS}
