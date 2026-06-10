package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should debit from account"
    request {
        method 'POST'
        url '/accounts/internal/debit'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            amount: 200.00
        ])
    }
    response {
        status 200
    }
}
