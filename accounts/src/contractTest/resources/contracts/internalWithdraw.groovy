package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should withdraw from account"
    request {
        method 'POST'
        url '/accounts/internal/withdraw'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            amount: 50.00
        ])
    }
    response {
        status 200
    }
}
