package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should deposit to account"
    request {
        method 'POST'
        url '/accounts/internal/deposit'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            amount: 100.00
        ])
    }
    response {
        status 200
    }
}
