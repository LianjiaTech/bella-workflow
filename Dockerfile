FROM openjdk:8

ENV IDC zeus
ENV MODULE bella-workflow
ENV ENVTYPE test
ENV DEBUGPORT 9008
ENV JMXPORT 9009
ENV MATRIX_CODE_DIR /opt/bella-workflow/htdocs
ENV MATRIX_APPLOGS_DIR /opt/bella-workflow/applogs
ENV MATRIX_ACCESSLOGS_DIR /opt/bella-workflow/logs
ENV MATRIX_LOGS_DIR /opt/bella-workflow/logs
ENV MATRIX_CACHE_DIR /opt/bella-workflow/cache
ENV MATRIX_PRIVDATA_DIR /opt/bella-workflow/privdata

COPY release/ /opt/bella-workflow/htdocs/
RUN chmod +x /opt/bella-workflow/htdocs/bin/*.sh

EXPOSE 8080 9008 9009
WORKDIR /opt/bella-workflow/htdocs
VOLUME ["/opt/bella-workflow/applogs", "/opt/bella-workflow/logs", "/opt/bella-workflow/cache", "/opt/bella-workflow/privdata"]
CMD ["/bin/bash", "-x", "/opt/bella-workflow/htdocs/bin/run.sh"]
