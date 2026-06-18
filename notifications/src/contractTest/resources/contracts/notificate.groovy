package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should log notification message"
    request {
        method 'POST'
        url '/notifications/notificate'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            message: "Test notification message"
        ])
    }
    response {
        status 200
    }
}
